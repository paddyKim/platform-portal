package com.paddykim.platform.portal.source;

import java.time.Instant;

public record SourceRepositoryResponse(
        Long id,
        String name,
        SourceRepositoryProvider provider,
        String repositoryUrl,
        String apiBaseUrl,
        String accountName,
        boolean credentialConfigured,
        String defaultBranch,
        String description,
        Instant createdAt
) {

    static SourceRepositoryResponse from(SourceRepository repository) {
        return new SourceRepositoryResponse(
                repository.getId(),
                repository.getName(),
                repository.getProvider(),
                repository.getRepositoryUrl(),
                repository.getApiBaseUrl(),
                repository.getAccountName(),
                repository.hasAccessToken(),
                repository.getDefaultBranch(),
                repository.getDescription(),
                repository.getCreatedAt()
        );
    }
}
