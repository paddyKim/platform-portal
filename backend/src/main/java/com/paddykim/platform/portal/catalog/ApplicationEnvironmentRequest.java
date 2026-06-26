package com.paddykim.platform.portal.catalog;

import jakarta.validation.constraints.NotBlank;

public record ApplicationEnvironmentRequest(
        @NotBlank String environment,
        @NotBlank String namespace,
        @NotBlank String argocdApplicationName,
        @NotBlank String helmValuesPath,
        @NotBlank String serviceUrl
) {
}
