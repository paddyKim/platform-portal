package com.paddykim.platform.portal.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplicationCommandRequest(
        @NotBlank
        @Size(max = 500)
        String command
) {
}
