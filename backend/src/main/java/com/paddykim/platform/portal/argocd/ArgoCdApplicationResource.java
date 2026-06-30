package com.paddykim.platform.portal.argocd;

public record ArgoCdApplicationResource(
        String kind,
        String name,
        String namespace,
        String syncStatus,
        String healthStatus,
        String hook,
        String syncWave
) {
}
