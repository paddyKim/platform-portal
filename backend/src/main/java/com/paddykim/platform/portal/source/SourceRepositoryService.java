package com.paddykim.platform.portal.source;

import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SourceRepositoryService {

    private final SourceRepositoryRepository sourceRepositoryRepository;

    public SourceRepositoryService(SourceRepositoryRepository sourceRepositoryRepository) {
        this.sourceRepositoryRepository = sourceRepositoryRepository;
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

        String accessToken = request.accessToken() == null ? "" : request.accessToken().trim();
        if (request.visibility() == SourceRepositoryVisibility.PRIVATE && accessToken.isBlank()) {
            throw new SourceRepositoryValidationException("Private repository requires an access token");
        }

        SourceRepository repository = sourceRepositoryRepository.save(new SourceRepository(
                request.name().trim(),
                request.provider(),
                request.visibility(),
                repositoryUrl,
                request.apiBaseUrl().trim(),
                request.accountName().trim(),
                accessToken,
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
