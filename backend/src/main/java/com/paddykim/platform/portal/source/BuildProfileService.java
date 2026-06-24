package com.paddykim.platform.portal.source;

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

    public BuildProfileService(
            SourceRepositoryRepository sourceRepositoryRepository,
            BuildProfileRepository buildProfileRepository,
            PlatformCicdExecutionClient platformCicdExecutionClient,
            SourceRepositoryCredentialService credentialService
    ) {
        this.sourceRepositoryRepository = sourceRepositoryRepository;
        this.buildProfileRepository = buildProfileRepository;
        this.platformCicdExecutionClient = platformCicdExecutionClient;
        this.credentialService = credentialService;
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

        BuildProfile buildProfile = buildProfileRepository.save(new BuildProfile(
                sourceRepository,
                values.name(),
                request.ciTool(),
                values.workingDirectory(),
                values.script(),
                values.description()
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

        buildProfile.update(
                values.name(),
                request.ciTool(),
                values.workingDirectory(),
                values.script(),
                values.description()
        );

        return BuildProfileResponse.from(buildProfile);
    }

    @Transactional
    public void deleteBuildProfile(Long sourceRepositoryId, Long buildProfileId) {
        BuildProfile buildProfile = findBuildProfile(sourceRepositoryId, buildProfileId);
        buildProfileRepository.delete(buildProfile);
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
        String imageTag = request.imageTag().trim();
        String branch = textOrDefault(request.branch(), "main");
        String credential = credentialService.decryptFromStorage(sourceRepository.getAccessToken());
        Long portalRequestId = Instant.now().toEpochMilli();
        PlatformCicdExecutionResponse execution = platformCicdExecutionClient.createExecution(
                new PlatformCicdExecutionCreateRequest(
                        portalRequestId,
                        textOrDefault(request.applicationName(), sourceRepository.getName()),
                        textOrDefault(request.environment(), "dev"),
                        textOrDefault(request.componentName(), sourceRepository.getName()),
                        "BUILD_IMAGE",
                        imageTag,
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
                )
        );
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
                sourceRepository.getName(),
                sourceRepository.getRepositoryUrl(),
                buildProfile.getCiTool(),
                buildProfile.getWorkingDirectory(),
                requestedBy,
                imageTag,
                branch,
                "platform-cicd-http",
                execution
        );
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

    private record BuildProfileValues(
            String name,
            String workingDirectory,
            String script,
            String description
    ) {
    }
}
