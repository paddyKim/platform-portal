package com.paddykim.platform.portal.argocd;

import jakarta.validation.constraints.NotNull;

public record ArgoCdApplicationSyncRequest(
        @NotNull Boolean prune,
        @NotNull Boolean dryRun
) {
}
