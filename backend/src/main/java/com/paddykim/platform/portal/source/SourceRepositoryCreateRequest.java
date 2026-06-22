package com.paddykim.platform.portal.source;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SourceRepositoryCreateRequest(
        @NotBlank String name,
        @NotNull SourceRepositoryProvider provider,
        @NotBlank String repositoryUrl,
        @NotBlank String apiBaseUrl,
        @NotBlank String accountName,
        @NotBlank String accessToken,
        @NotBlank String defaultBranch,
        @NotBlank String description
) {
}
