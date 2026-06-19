package com.paddykim.platform.portal.argocd;

import java.util.List;

public record ArgoCdStatusResponse(
        Long applicationId,
        String applicationName,
        String environment,
        String argocdApplicationName,
        String connectionStatus,
        String syncStatus,
        String healthStatus,
        String operationPhase,
        String reconciledAt,
        List<String> images,
        String message
) {

    static ArgoCdStatusResponse available(
            Long applicationId,
            String applicationName,
            String environment,
            String argocdApplicationName,
            ArgoCdApplicationSnapshot snapshot
    ) {
        return new ArgoCdStatusResponse(
                applicationId,
                applicationName,
                environment,
                argocdApplicationName,
                "AVAILABLE",
                snapshot.syncStatus(),
                snapshot.healthStatus(),
                snapshot.operationPhase(),
                snapshot.reconciledAt(),
                snapshot.images(),
                null
        );
    }

    static ArgoCdStatusResponse unavailable(
            Long applicationId,
            String applicationName,
            String environment,
            String argocdApplicationName,
            String message
    ) {
        return new ArgoCdStatusResponse(
                applicationId,
                applicationName,
                environment,
                argocdApplicationName,
                "UNAVAILABLE",
                "Unknown",
                "Unknown",
                "Unknown",
                null,
                List.of(),
                message
        );
    }
}
