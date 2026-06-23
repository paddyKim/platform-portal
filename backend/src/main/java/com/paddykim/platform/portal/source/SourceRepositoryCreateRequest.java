package com.paddykim.platform.portal.source;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SourceRepositoryCreateRequest(
        @NotBlank String name,
        @NotNull SourceRepositoryProvider provider,
        @NotNull SourceRepositoryVisibility visibility,
        @NotBlank String repositoryUrl,
        String apiBaseUrl,
        @NotBlank String accountName,
        @NotBlank String encryptedAccessToken,
        String description
) {
}
