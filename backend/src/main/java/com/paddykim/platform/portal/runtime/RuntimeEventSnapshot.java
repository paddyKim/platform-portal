package com.paddykim.platform.portal.runtime;

public record RuntimeEventSnapshot(
        String type,
        String reason,
        String objectKind,
        String objectName,
        String message,
        String lastSeenAt
) {
}
