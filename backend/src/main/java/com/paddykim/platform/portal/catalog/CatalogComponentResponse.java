package com.paddykim.platform.portal.catalog;

public record CatalogComponentResponse(
        Long id,
        String name,
        String kind,
        String deploymentName,
        String serviceName,
        String imageRepository,
        CatalogManifestMappingResponse manifestMapping
) {

    static CatalogComponentResponse from(ApplicationComponent component) {
        return new CatalogComponentResponse(
                component.getId(),
                component.getName(),
                component.getKind(),
                component.getDeploymentName(),
                component.getServiceName(),
                component.getImageRepository(),
                CatalogManifestMappingResponse.from(component.getManifestMapping())
        );
    }
}
