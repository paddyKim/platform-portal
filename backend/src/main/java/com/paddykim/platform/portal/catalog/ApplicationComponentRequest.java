package com.paddykim.platform.portal.catalog;

import jakarta.validation.constraints.NotBlank;

public record ApplicationComponentRequest(
        @NotBlank String name,
        @NotBlank String kind,
        @NotBlank String deploymentName,
        @NotBlank String serviceName,
        @NotBlank String imageRepository
) {
}
