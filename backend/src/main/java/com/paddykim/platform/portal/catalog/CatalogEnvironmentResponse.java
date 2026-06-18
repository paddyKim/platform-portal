package com.paddykim.platform.portal.catalog;

import java.util.Comparator;
import java.util.List;

public record CatalogEnvironmentResponse(
        Long id,
        String environment,
        String namespace,
        String argocdApplicationName,
        String helmValuesPath,
        String serviceUrl,
        List<CatalogComponentResponse> components
) {

    static CatalogEnvironmentResponse from(ApplicationEnvironment environment, boolean includeComponents) {
        List<CatalogComponentResponse> components = includeComponents
                ? environment.getComponents().stream()
                        .sorted(Comparator.comparing(ApplicationComponent::getName))
                        .map(CatalogComponentResponse::from)
                        .toList()
                : List.of();

        return new CatalogEnvironmentResponse(
                environment.getId(),
                environment.getEnvironment(),
                environment.getNamespace(),
                environment.getArgocdApplicationName(),
                environment.getHelmValuesPath(),
                environment.getServiceUrl(),
                components
        );
    }
}
