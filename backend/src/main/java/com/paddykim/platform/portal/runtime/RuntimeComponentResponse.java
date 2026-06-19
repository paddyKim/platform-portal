package com.paddykim.platform.portal.runtime;

import java.util.List;

public record RuntimeComponentResponse(
        Long componentId,
        String componentName,
        String kind,
        String status,
        String deploymentName,
        int desiredReplicas,
        int readyReplicas,
        int availableReplicas,
        int updatedReplicas,
        String serviceName,
        String serviceType,
        String clusterIp,
        List<String> servicePorts,
        List<String> images,
        List<RuntimePodSnapshot> pods,
        List<RuntimeEventSnapshot> recentEvents,
        int restartCount,
        int warningEvents,
        String message
) {

    static RuntimeComponentResponse from(RuntimeComponentSnapshot snapshot) {
        int restartCount = snapshot.pods().stream()
                .mapToInt(RuntimePodSnapshot::restartCount)
                .sum();
        int warningEvents = (int) snapshot.recentEvents().stream()
                .filter(event -> "Warning".equalsIgnoreCase(event.type()))
                .count();

        return new RuntimeComponentResponse(
                snapshot.componentId(),
                snapshot.componentName(),
                snapshot.kind(),
                snapshot.status(),
                snapshot.deploymentName(),
                snapshot.desiredReplicas(),
                snapshot.readyReplicas(),
                snapshot.availableReplicas(),
                snapshot.updatedReplicas(),
                snapshot.serviceName(),
                snapshot.serviceType(),
                snapshot.clusterIp(),
                snapshot.servicePorts(),
                snapshot.images(),
                snapshot.pods(),
                snapshot.recentEvents(),
                restartCount,
                warningEvents,
                snapshot.message()
        );
    }
}
