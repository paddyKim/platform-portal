package com.paddykim.platform.portal.runtime;

import java.util.List;

public record RuntimeStatusResponse(
        Long applicationId,
        String applicationName,
        String environment,
        String namespace,
        String connectionStatus,
        RuntimeSummaryResponse summary,
        List<RuntimeComponentResponse> components,
        String message
) {

    static RuntimeStatusResponse available(
            Long applicationId,
            String applicationName,
            String environment,
            String namespace,
            KubernetesRuntimeSnapshot snapshot
    ) {
        List<RuntimeComponentResponse> components = snapshot.components().stream()
                .map(RuntimeComponentResponse::from)
                .toList();

        return new RuntimeStatusResponse(
                applicationId,
                applicationName,
                environment,
                namespace,
                "AVAILABLE",
                RuntimeSummaryResponse.from(components),
                components,
                null
        );
    }

    static RuntimeStatusResponse unavailable(
            Long applicationId,
            String applicationName,
            String environment,
            String namespace,
            String message
    ) {
        return new RuntimeStatusResponse(
                applicationId,
                applicationName,
                environment,
                namespace,
                "UNAVAILABLE",
                RuntimeSummaryResponse.empty(),
                List.of(),
                message
        );
    }
}
