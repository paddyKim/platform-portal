package com.paddykim.platform.portal.source;

record BuildProfileTarget(
        Long applicationId,
        Long environmentId,
        Long componentId,
        String applicationName,
        String environment,
        String componentName,
        String imageRepository,
        String helmValuesPath,
        String argocdApplicationName
) {
}
