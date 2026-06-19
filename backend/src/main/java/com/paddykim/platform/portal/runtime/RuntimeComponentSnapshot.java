package com.paddykim.platform.portal.runtime;

import java.util.List;

public record RuntimeComponentSnapshot(
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
        String message
) {
}
