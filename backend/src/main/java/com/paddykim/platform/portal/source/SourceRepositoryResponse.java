package com.paddykim.platform.portal.source;

import java.time.Instant;

public record SourceRepositoryResponse(
        Long id,
        String name,
        String repositoryUrl,
        String defaultBranch,
        String description,
        Instant createdAt
) {

    static SourceRepositoryResponse from(SourceRepository repository) {
        return new SourceRepositoryResponse(
                repository.getId(),
                repository.getName(),
                repository.getRepositoryUrl(),
                repository.getDefaultBranch(),
                repository.getDescription(),
                repository.getCreatedAt()
        );
    }
}
