package com.paddykim.platform.portal.argocd;

import java.util.List;

public record ArgoCdApplicationSnapshot(
        String syncStatus,
        String healthStatus,
        String operationPhase,
        String reconciledAt,
        List<String> images
) {
}
