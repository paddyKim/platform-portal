package com.paddykim.platform.portal.source;

import java.time.Instant;

public record SourceRepositoryResponse(
        Long id,
        String name,
        SourceRepositoryProvider provider,
        SourceRepositoryVisibility visibility,
        String repositoryUrl,
        String apiBaseUrl,
        String accountName,
        boolean credentialConfigured,
        String description,
        long cloneCount,
        long buildCount,
        Instant lastClonedAt,
        Instant lastBuiltAt,
        Instant createdAt
) {

    static SourceRepositoryResponse from(SourceRepository repository) {
        return new SourceRepositoryResponse(
                repository.getId(),
                repository.getName(),
                repository.getProvider(),
                repository.getVisibility(),
                repository.getRepositoryUrl(),
                repository.getApiBaseUrl(),
                repository.getAccountName(),
                repository.hasAccessToken(),
                repository.getDescription(),
                repository.getCloneCount(),
                repository.getBuildCount(),
                repository.getLastClonedAt(),
                repository.getLastBuiltAt(),
                repository.getCreatedAt()
        );
    }
}
