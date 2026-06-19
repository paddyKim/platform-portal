package com.paddykim.platform.portal.argocd;

import java.util.List;
import java.util.Map;

public final class ArgoCdStatusMapper {

    private ArgoCdStatusMapper() {
    }

    public static ArgoCdApplicationSnapshot from(Map<String, Object> application) {
        Map<String, Object> status = asMap(application.get("status"));
        Map<String, Object> sync = asMap(status.get("sync"));
        Map<String, Object> health = asMap(status.get("health"));
        Map<String, Object> operationState = asMap(status.get("operationState"));
        Map<String, Object> summary = asMap(status.get("summary"));

        Object images = summary.get("images");

        return new ArgoCdApplicationSnapshot(
                stringOrUnknown(sync.get("status")),
                stringOrUnknown(health.get("status")),
                stringOrUnknown(operationState.get("phase")),
                stringOrNull(status.get("reconciledAt")),
                images instanceof List<?> imageList
                        ? imageList.stream().map(String::valueOf).toList()
                        : List.of()
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    private static String stringOrUnknown(Object value) {
        String result = stringOrNull(value);
        return result == null || result.isBlank() ? "Unknown" : result;
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
