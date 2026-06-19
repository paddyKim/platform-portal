package com.paddykim.platform.portal.cicd;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CicdRequestCreateRequest(
        @NotNull Long applicationId,
        @NotBlank String environment,
        @NotNull Long componentId,
        @NotNull CicdRequestType requestType,
        @NotBlank String requestedValue,
        @NotBlank String requestedBy
) {
}
