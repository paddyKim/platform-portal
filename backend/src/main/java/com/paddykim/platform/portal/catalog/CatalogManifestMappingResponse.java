package com.paddykim.platform.portal.catalog;

import java.time.Instant;

public record CatalogManifestMappingResponse(
        Long id,
        String manifestRepositoryUrl,
        String manifestBranch,
        String valuesPath,
        String imageTagKey,
        Instant createdAt,
        Instant updatedAt
) {

    static CatalogManifestMappingResponse from(ApplicationManifestMapping mapping) {
        if (mapping == null) {
            return null;
        }

        return new CatalogManifestMappingResponse(
                mapping.getId(),
                mapping.getManifestRepositoryUrl(),
                mapping.getManifestBranch(),
                mapping.getValuesPath(),
                mapping.getImageTagKey(),
                mapping.getCreatedAt(),
                mapping.getUpdatedAt()
        );
    }
}
