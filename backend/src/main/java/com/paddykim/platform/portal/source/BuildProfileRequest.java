package com.paddykim.platform.portal.source;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BuildProfileRequest(
        @NotBlank String name,
        @NotNull BuildProfileCiTool ciTool,
        @NotBlank String workingDirectory,
        @NotBlank String script,
        String description
) {
}
