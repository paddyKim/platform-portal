package com.paddykim.platform.portal.source;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceRepositoryService {

    private final SourceRepositoryRepository sourceRepositoryRepository;
    private final SourceRepositoryCredentialService credentialService;

    public SourceRepositoryService(
            SourceRepositoryRepository sourceRepositoryRepository,
            SourceRepositoryCredentialService credentialService
    ) {
        this.sourceRepositoryRepository = sourceRepositoryRepository;
        this.credentialService = credentialService;
    }

    @Transactional(readOnly = true)
    public List<SourceRepositoryResponse> listRepositories() {
        return sourceRepositoryRepository.findAll().stream()
                .sorted(Comparator.comparing(SourceRepository::getCreatedAt).reversed())
                .map(SourceRepositoryResponse::from)
                .toList();
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
                request.apiBaseUrl().trim(),
                request.accountName().trim(),
                encryptedAccessToken,
                "",
                request.description().trim()
        ));

        return SourceRepositoryResponse.from(repository);
    }

    @Transactional
    public void deleteRepository(Long id) {
        if (!sourceRepositoryRepository.existsById(id)) {
            throw new SourceRepositoryNotFoundException(id);
        }

        sourceRepositoryRepository.deleteById(id);
    }
}
