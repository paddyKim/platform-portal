package com.paddykim.platform.portal.catalog;

import jakarta.validation.constraints.NotBlank;

public record ApplicationManifestMappingRequest(
        @NotBlank String manifestRepositoryUrl,
        @NotBlank String manifestBranch,
        @NotBlank String valuesPath,
        @NotBlank String imageTagKey
) {
}
