package com.paddykim.platform.portal.source;

import jakarta.validation.constraints.NotBlank;

public record BuildProfileRunRequest(
        @NotBlank String requestedBy,
        @NotBlank String imageTag
) {
}
