package com.paddykim.platform.portal.runtime;

public record RuntimePodSnapshot(
        String name,
        String phase,
        int readyContainers,
        int totalContainers,
        int restartCount,
        String podIp,
        String nodeName,
        String startedAt
) {
}
