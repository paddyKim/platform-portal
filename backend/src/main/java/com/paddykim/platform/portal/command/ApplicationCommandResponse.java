package com.paddykim.platform.portal.command;

public record ApplicationCommandResponse(
        String intent,
        String view,
        String message,
        double confidence,
        String resultApiMethod,
        String resultApiPath
) {
}
