package com.paddykim.platform.portal.runtime;

import java.util.List;

public record RuntimeSummaryResponse(
        int desiredReplicas,
        int readyReplicas,
        int availableReplicas,
        int warningEvents
) {

    static RuntimeSummaryResponse from(List<RuntimeComponentResponse> components) {
        int desiredReplicas = components.stream().mapToInt(RuntimeComponentResponse::desiredReplicas).sum();
        int readyReplicas = components.stream().mapToInt(RuntimeComponentResponse::readyReplicas).sum();
        int availableReplicas = components.stream().mapToInt(RuntimeComponentResponse::availableReplicas).sum();
        int warningEvents = components.stream().mapToInt(component -> component.warningEvents()).sum();

        return new RuntimeSummaryResponse(desiredReplicas, readyReplicas, availableReplicas, warningEvents);
    }

    static RuntimeSummaryResponse empty() {
        return new RuntimeSummaryResponse(0, 0, 0, 0);
    }
}
