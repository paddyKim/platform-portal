package com.paddykim.platform.portal.argocd;

public record ArgoCdApplicationCondition(
        String type,
        String message,
        String lastTransitionTime
) {
}
