package com.paddykim.platform.portal.argocd;

public record ArgoCdApplicationSummary(
        String name,
        String argocdNamespace,
        String project,
        String sourceRepoUrl,
        String sourcePath,
        String targetRevision,
        String destinationServer,
        String destinationNamespace,
        String syncStatus,
        String healthStatus,
        String operationPhase,
        String reconciledAt
) {
}
