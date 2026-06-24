package com.paddykim.platform.portal.source;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceRepositoryService {

    private final SourceRepositoryRepository sourceRepositoryRepository;
    private final BuildProfileRepository buildProfileRepository;
    private final BuildExecutionHistoryRepository buildExecutionHistoryRepository;
    private final SourceRepositoryCredentialService credentialService;

    public SourceRepositoryService(
            SourceRepositoryRepository sourceRepositoryRepository,
            BuildProfileRepository buildProfileRepository,
            BuildExecutionHistoryRepository buildExecutionHistoryRepository,
            SourceRepositoryCredentialService credentialService
    ) {
        this.sourceRepositoryRepository = sourceRepositoryRepository;
        this.buildProfileRepository = buildProfileRepository;
        this.buildExecutionHistoryRepository = buildExecutionHistoryRepository;
        this.credentialService = credentialService;
    }

    @Transactional(readOnly = true)
    public List<SourceRepositoryResponse> listRepositories() {
        return sourceRepositoryRepository.findAll().stream()
                .sorted(Comparator.comparing(SourceRepository::getCreatedAt).reversed())
                .map(SourceRepositoryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SourceRepositoryResponse getRepository(Long id) {
        SourceRepository repository = sourceRepositoryRepository.findById(id)
                .orElseThrow(() -> new SourceRepositoryNotFoundException(id));

        return SourceRepositoryResponse.from(repository);
    }

    @Transactional
    public SourceRepositoryResponse createRepository(SourceRepositoryCreateRequest request) {
        String repositoryUrl = request.repositoryUrl().trim();
        if (sourceRepositoryRepository.existsByRepositoryUrl(repositoryUrl)) {
            throw new SourceRepositoryValidationException("Source repository already registered: " + repositoryUrl);
        }

        String accessToken = credentialService.decryptFromNetwork(request.encryptedAccessToken().trim()).trim();
        if (accessToken.isBlank()) {
            throw new SourceRepositoryValidationException("Source repository credential must not be blank");
        }
        String encryptedAccessToken = credentialService.encryptForStorage(accessToken);

        SourceRepository repository = sourceRepositoryRepository.save(new SourceRepository(
                request.name().trim(),
                request.provider(),
                request.visibility(),
                repositoryUrl,
                apiBaseUrl(request),
                request.accountName().trim(),
                encryptedAccessToken,
                "",
                description(request)
        ));

        return SourceRepositoryResponse.from(repository);
    }

    private static String description(SourceRepositoryCreateRequest request) {
        return request.description() == null ? "" : request.description().trim();
    }

    private static String apiBaseUrl(SourceRepositoryCreateRequest request) {
        if (request.apiBaseUrl() != null && !request.apiBaseUrl().isBlank()) {
            return request.apiBaseUrl().trim();
        }

        return switch (request.provider()) {
            case GITLAB -> "https://gitlab.com/api/v4";
            case GITHUB -> "https://api.github.com";
        };
    }

    @Transactional
    public void deleteRepository(Long id) {
        if (!sourceRepositoryRepository.existsById(id)) {
            throw new SourceRepositoryNotFoundException(id);
        }

        buildExecutionHistoryRepository.deleteBySourceRepositoryId(id);
        buildProfileRepository.deleteBySourceRepositoryId(id);
        sourceRepositoryRepository.deleteById(id);
    }
}
