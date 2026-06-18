package com.paddykim.platform.portal.catalog;

import java.util.Comparator;
import java.util.List;

public record CatalogApplicationResponse(
        Long id,
        String name,
        String description,
        String owner,
        String repositoryUrl,
        List<CatalogEnvironmentResponse> environments
) {

    static CatalogApplicationResponse from(Application application, boolean includeComponents) {
        return new CatalogApplicationResponse(
                application.getId(),
                application.getName(),
                application.getDescription(),
                application.getOwner(),
                application.getRepositoryUrl(),
                application.getEnvironments().stream()
                        .sorted(Comparator.comparing(ApplicationEnvironment::getEnvironment))
                        .map(environment -> CatalogEnvironmentResponse.from(environment, includeComponents))
                        .toList()
        );
    }
}
