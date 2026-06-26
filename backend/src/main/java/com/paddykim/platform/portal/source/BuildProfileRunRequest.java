package com.paddykim.platform.portal.source;

import jakarta.validation.constraints.NotBlank;

public record BuildProfileRunRequest(
        @NotBlank String requestedBy,
        String imageTag,
        String branch,
        String applicationName,
        String environment,
        String componentName
) {
}
