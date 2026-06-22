package com.paddykim.platform.portal.source;

import jakarta.validation.constraints.NotBlank;

public record SourceRepositoryCreateRequest(
        @NotBlank String name,
        @NotBlank String repositoryUrl,
        @NotBlank String defaultBranch,
        @NotBlank String description
) {
}
