package com.paddykim.platform.portal.source;

import com.paddykim.platform.portal.catalog.Application;
import com.paddykim.platform.portal.catalog.ApplicationComponent;
import com.paddykim.platform.portal.catalog.ApplicationEnvironment;
import com.paddykim.platform.portal.catalog.ApplicationRepository;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuildProfileService {

    private final SourceRepositoryRepository sourceRepositoryRepository;
    private final BuildProfileRepository buildProfileRepository;
    private final PlatformCicdExecutionClient platformCicdExecutionClient;
    private final SourceRepositoryCredentialService credentialService;
    private final BuildExecutionHistoryRepository buildExecutionHistoryRepository;
    private final ApplicationRepository applicationRepository;

    public BuildProfileService(
            SourceRepositoryRepository sourceRepositoryRepository,
            BuildProfileRepository buildProfileRepository,
            PlatformCicdExecutionClient platformCicdExecutionClient,
            SourceRepositoryCredentialService credentialService,
            BuildExecutionHistoryRepository buildExecutionHistoryRepository,
            ApplicationRepository applicationRepository
    ) {
        this.sourceRepositoryRepository = sourceRepositoryRepository;
        this.buildProfileRepository = buildProfileRepository;
        this.platformCicdExecutionClient = platformCicdExecutionClient;
        this.credentialService = credentialService;
        this.buildExecutionHistoryRepository = buildExecutionHistoryRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public List<BuildProfileResponse> listBuildProfiles(Long sourceRepositoryId) {
        ensureSourceRepositoryExists(sourceRepositoryId);

        return buildProfileRepository.findBySourceRepositoryId(sourceRepositoryId).stream()
                .sorted(Comparator.comparing(BuildProfile::getCreatedAt).reversed())
                .map(BuildProfileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BuildProfileResponse getBuildProfile(Long sourceRepositoryId, Long buildProfileId) {
        return BuildProfileResponse.from(findBuildProfile(sourceRepositoryId, buildProfileId));
    }

    @Transactional
    public BuildProfileResponse createBuildProfile(Long sourceRepositoryId, BuildProfileRequest request) {
        SourceRepository sourceRepository = sourceRepositoryRepository.findById(sourceRepositoryId)
                .orElseThrow(() -> new SourceRepositoryNotFoundException(sourceRepositoryId));
        BuildProfileValues values = validate(request);
        BuildProfileTarget target = resolveTarget(request);

        BuildProfile buildProfile = buildProfileRepository.save(new BuildProfile(
                sourceRepository,
                values.name(),
                request.ciTool(),
                values.workingDirectory(),
                values.script(),
                values.description(),
                target
        ));

        return BuildProfileResponse.from(buildProfile);
    }

    @Transactional
    public BuildProfileResponse updateBuildProfile(
            Long sourceRepositoryId,
            Long buildProfileId,
            BuildProfileRequest request
    ) {
        BuildProfile buildProfile = findBuildProfile(sourceRepositoryId, buildProfileId);
        BuildProfileValues values = validate(request);
        BuildProfileTarget target = resolveTarget(request);

        buildProfile.update(
                values.name(),
                request.ciTool(),
                values.workingDirectory(),
                values.script(),
                values.description(),
                target
        );

        return BuildProfileResponse.from(buildProfile);
    }

    @Transactional
    public void deleteBuildProfile(Long sourceRepositoryId, Long buildProfileId) {
        BuildProfile buildProfile = findBuildProfile(sourceRepositoryId, buildProfileId);
        buildExecutionHistoryRepository.deleteByBuildProfileId(buildProfileId);
        buildProfileRepository.delete(buildProfile);
    }

    @Transactional(readOnly = true)
    public List<BuildExecutionHistoryResponse> listBuildProfileExecutions(
            Long sourceRepositoryId,
            Long buildProfileId
    ) {
        findBuildProfile(sourceRepositoryId, buildProfileId);

        return buildExecutionHistoryRepository
                .findBySourceRepositoryIdAndBuildProfileIdOrderByCreatedAtDesc(sourceRepositoryId, buildProfileId)
                .stream()
                .map(BuildExecutionHistoryResponse::from)
                .toList();
    }

    @Transactional
    public BuildProfileRunResponse prepareBuildProfileRun(
            Long sourceRepositoryId,
            Long buildProfileId,
            BuildProfileRunRequest request
    ) {
        BuildProfile buildProfile = findBuildProfile(sourceRepositoryId, buildProfileId);
        SourceRepository sourceRepository = buildProfile.getSourceRepository();
        String requestedBy = request.requestedBy().trim();
        String requestedValue = textOrDefault(request.imageTag(), "artifact-output");
        String branch = textOrDefault(request.branch(), "main");
        String credential = credentialService.decryptFromStorage(sourceRepository.getAccessToken());
        Long portalRequestId = Instant.now().toEpochMilli();
        String applicationName = textOrDefault(buildProfile.getTargetApplicationName(), sourceRepository.getName());
        String environment = textOrDefault(buildProfile.getTargetEnvironment(), "dev");
        String componentName = textOrDefault(buildProfile.getTargetComponentName(), sourceRepository.getName());
        PlatformCicdExecutionResponse execution;
        try {
            execution = platformCicdExecutionClient.createExecution(new PlatformCicdExecutionCreateRequest(
                    portalRequestId,
                    applicationName,
                    environment,
                    componentName,
                    "BUILD_IMAGE",
                    requestedValue,
                    requestedBy,
                    sourceRepositoryId,
                    buildProfileId,
                    buildProfile.getCiTool().name(),
                    sourceRepository.getRepositoryUrl(),
                    branch,
                    sourceRepository.getAccountName(),
                    credential,
                    buildProfile.getWorkingDirectory(),
                    buildProfile.getScript()
            ));
        } catch (SourceRepositoryValidationException exception) {
            BuildExecutionHistory history = saveFailedHistory(
                    sourceRepository,
                    buildProfile,
                    portalRequestId,
                    branch,
                    requestedBy,
                    requestedValue,
                    exception.getMessage()
            );

            return BuildProfileRunResponse.failed(
                    sourceRepositoryId,
                    buildProfileId,
                    buildProfile.getName(),
                    history.getId(),
                    sourceRepository.getName(),
                    sourceRepository.getRepositoryUrl(),
                    buildProfile.getCiTool(),
                    buildProfile.getWorkingDirectory(),
                    requestedBy,
                    requestedValue,
                    branch,
                    "platform-cicd-http",
                    portalRequestId,
                    exception.getMessage()
            );
        }

        BuildExecutionHistory history = saveHistory(sourceRepository, buildProfile, branch, requestedBy, requestedValue, execution);
        // Manifest update is deferred until application manifest ownership is modeled explicitly.
        // recordManifestUpdate(history, buildProfile, requestedBy);
        Instant now = Instant.now();
        if ("SUCCEEDED".equalsIgnoreCase(execution.cloneStatus())) {
            sourceRepository.markCloned(now);
        }
        if (execution.finishedAt() != null) {
            sourceRepository.markBuilt(execution.finishedAt());
        }

        return BuildProfileRunResponse.from(
                sourceRepositoryId,
                buildProfileId,
                buildProfile.getName(),
                sourceRepository.getName(),
                sourceRepository.getRepositoryUrl(),
                buildProfile.getCiTool(),
                buildProfile.getWorkingDirectory(),
                requestedBy,
                requestedValue,
                branch,
                "platform-cicd-http",
                history.getId(),
                execution,
                history
        );
    }

    private void recordManifestUpdate(
            BuildExecutionHistory history,
            BuildProfile buildProfile,
            String requestedBy
    ) {
        if (!"SUCCEEDED".equalsIgnoreCase(history.getStatus())) {
            return;
        }
        if (buildProfile.getTargetComponentId() == null) {
            history.recordManifestUpdate("FAILED", "Build profile target is not configured", null, Instant.now());
            return;
        }
        if (history.getImageRepository() == null || history.getImageTag() == null || history.getImageReference() == null) {
            history.recordManifestUpdate("FAILED", "Build artifact image output is missing", null, Instant.now());
            return;
        }
        if (!history.getImageRepository().equals(buildProfile.getTargetImageRepository())) {
            history.recordManifestUpdate(
                    "FAILED",
                    "Artifact imageRepository does not match target component imageRepository: %s != %s".formatted(
                            history.getImageRepository(),
                            buildProfile.getTargetImageRepository()
                    ),
                    null,
                    Instant.now()
            );
            return;
        }

        try {
            PlatformCicdExecutionResponse deployExecution = platformCicdExecutionClient.createExecution(
                    new PlatformCicdExecutionCreateRequest(
                            Instant.now().toEpochMilli(),
                            buildProfile.getTargetApplicationName(),
                            buildProfile.getTargetEnvironment(),
                            buildProfile.getTargetComponentName(),
                            "DEPLOY_IMAGE",
                            history.getImageTag(),
                            requestedBy,
                            history.getSourceRepository().getId(),
                            buildProfile.getId(),
                            buildProfile.getCiTool().name(),
                            history.getSourceRepository().getRepositoryUrl(),
                            history.getBranch(),
                            history.getSourceRepository().getAccountName(),
                            null,
                            buildProfile.getWorkingDirectory(),
                            null
                    )
            );
            history.recordManifestUpdate(
                    nullToDefault(deployExecution.status(), "UNKNOWN"),
                    deployExecution.statusMessage(),
                    deployExecution.changedFilePath(),
                    deployExecution.finishedAt() == null ? Instant.now() : deployExecution.finishedAt()
            );
        } catch (SourceRepositoryValidationException exception) {
            history.recordManifestUpdate("FAILED", exception.getMessage(), null, Instant.now());
        }
    }

    private BuildExecutionHistory saveHistory(
            SourceRepository sourceRepository,
            BuildProfile buildProfile,
            String branch,
            String requestedBy,
            String imageTag,
            PlatformCicdExecutionResponse execution
    ) {
        return buildExecutionHistoryRepository.save(new BuildExecutionHistory(
                sourceRepository,
                buildProfile,
                execution.executionId(),
                execution.portalRequestId(),
                buildProfile.getCiTool(),
                branch,
                requestedBy,
                imageTag,
                nullToDefault(execution.status(), "UNKNOWN"),
                execution.statusMessage(),
                execution.cloneStatus(),
                execution.cloneMessage(),
                execution.checkoutPath(),
                execution.startedAt(),
                execution.finishedAt(),
                execution.exitCode(),
                execution.logSummary(),
                null,
                null,
                execution.imageRepository(),
                execution.imageTag(),
                execution.imageDigest(),
                execution.imageReference()
        ));
    }

    private BuildExecutionHistory saveFailedHistory(
            SourceRepository sourceRepository,
            BuildProfile buildProfile,
            Long portalRequestId,
            String branch,
            String requestedBy,
            String imageTag,
            String statusMessage
    ) {
        return buildExecutionHistoryRepository.save(new BuildExecutionHistory(
                sourceRepository,
                buildProfile,
                null,
                portalRequestId,
                buildProfile.getCiTool(),
                branch,
                requestedBy,
                imageTag,
                "FAILED",
                statusMessage,
                null,
                null,
                null,
                null,
                null,
                null,
                "",
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    private BuildProfile findBuildProfile(Long sourceRepositoryId, Long buildProfileId) {
        ensureSourceRepositoryExists(sourceRepositoryId);

        return buildProfileRepository.findByIdAndSourceRepositoryId(buildProfileId, sourceRepositoryId)
                .orElseThrow(() -> new BuildProfileNotFoundException(sourceRepositoryId, buildProfileId));
    }

    private void ensureSourceRepositoryExists(Long sourceRepositoryId) {
        if (!sourceRepositoryRepository.existsById(sourceRepositoryId)) {
            throw new SourceRepositoryNotFoundException(sourceRepositoryId);
        }
    }

    private BuildProfileTarget resolveTarget(BuildProfileRequest request) {
        Application application = applicationRepository.findWithEnvironmentsById(request.targetApplicationId())
                .orElseThrow(() -> new SourceRepositoryValidationException(
                        "Target application not found: " + request.targetApplicationId()
                ));

        ApplicationEnvironment environment = application.getEnvironments().stream()
                .filter(candidate -> candidate.getId().equals(request.targetEnvironmentId()))
                .findFirst()
                .orElseThrow(() -> new SourceRepositoryValidationException(
                        "Target environment does not belong to application: " + request.targetEnvironmentId()
                ));

        ApplicationComponent component = environment.getComponents().stream()
                .filter(candidate -> candidate.getId().equals(request.targetComponentId()))
                .findFirst()
                .orElseThrow(() -> new SourceRepositoryValidationException(
                        "Target component does not belong to environment: " + request.targetComponentId()
                ));

        return new BuildProfileTarget(
                application.getId(),
                environment.getId(),
                component.getId(),
                application.getName(),
                environment.getEnvironment(),
                component.getName(),
                component.getImageRepository(),
                environment.getHelmValuesPath(),
                environment.getArgocdApplicationName()
        );
    }

    private static BuildProfileValues validate(BuildProfileRequest request) {
        String name = request.name().trim();
        String workingDirectory = request.workingDirectory().trim();
        String script = request.script().trim();
        String description = request.description() == null ? "" : request.description().trim();

        validateWorkingDirectory(workingDirectory);

        return new BuildProfileValues(name, workingDirectory, script, description);
    }

    private static void validateWorkingDirectory(String workingDirectory) {
        try {
            Path path = Path.of(workingDirectory).normalize();
            if (path.isAbsolute() || path.startsWith("..") || workingDirectory.contains("..")) {
                throw new SourceRepositoryValidationException("Build profile workingDirectory must stay inside repository");
            }
        } catch (InvalidPathException exception) {
            throw new SourceRepositoryValidationException("Build profile workingDirectory is invalid");
        }
    }

    private static String textOrDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private static String nullToDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        return value.trim();
    }

    private record BuildProfileValues(
            String name,
            String workingDirectory,
            String script,
            String description
    ) {
    }
}
