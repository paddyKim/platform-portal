package com.paddykim.platform.portal.command;

import java.util.Map;

public record ApplicationCommandResponse(
        String intent,
        String view,
        String message,
        double confidence,
        String resultApiMethod,
        String resultApiPath,
        Map<String, String> parameters
) {
}
