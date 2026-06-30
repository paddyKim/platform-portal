package com.paddykim.platform.portal.argocd;

import java.util.List;

public record ArgoCdApplicationDetail(
        String name,
        String argocdNamespace,
        String project,
        String sourceRepoUrl,
        String sourcePath,
        String targetRevision,
        String destinationServer,
        String destinationNamespace,
        String syncStatus,
        String syncRevision,
        String healthStatus,
        String operationPhase,
        String operationMessage,
        String reconciledAt,
        List<String> images,
        List<ArgoCdApplicationResource> resources,
        List<ArgoCdApplicationCondition> conditions
) {
}
