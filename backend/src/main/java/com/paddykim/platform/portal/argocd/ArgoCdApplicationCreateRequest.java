package com.paddykim.platform.portal.argocd;

import jakarta.validation.constraints.NotBlank;

public record ArgoCdApplicationCreateRequest(
        @NotBlank String name,
        @NotBlank String project,
        @NotBlank String sourceRepoUrl,
        @NotBlank String sourcePath,
        @NotBlank String targetRevision,
        @NotBlank String destinationServer,
        @NotBlank String destinationNamespace,
        boolean automated,
        boolean prune,
        boolean selfHeal
) {
}
