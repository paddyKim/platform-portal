package com.paddykim.platform.portal.catalog;

import jakarta.validation.constraints.NotBlank;

public record ApplicationRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotBlank String owner,
        @NotBlank String repositoryUrl
) {
}
