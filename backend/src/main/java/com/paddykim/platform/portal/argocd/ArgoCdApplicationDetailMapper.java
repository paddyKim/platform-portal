package com.paddykim.platform.portal.argocd;

import java.util.List;
import java.util.Map;

public final class ArgoCdApplicationDetailMapper {

    private ArgoCdApplicationDetailMapper() {
    }

    public static ArgoCdApplicationDetail from(
            String name,
            String namespace,
            Map<String, Object> application
    ) {
        Map<String, Object> spec = asMap(application.get("spec"));
        Map<String, Object> source = asMap(spec.get("source"));
        Map<String, Object> destination = asMap(spec.get("destination"));
        Map<String, Object> status = asMap(application.get("status"));
        Map<String, Object> sync = asMap(status.get("sync"));
        Map<String, Object> health = asMap(status.get("health"));
        Map<String, Object> operationState = asMap(status.get("operationState"));
        Map<String, Object> summary = asMap(status.get("summary"));

        return new ArgoCdApplicationDetail(
                name,
                namespace,
                stringOrUnknown(spec.get("project")),
                stringOrUnknown(source.get("repoURL")),
                stringOrUnknown(source.get("path")),
                stringOrUnknown(source.get("targetRevision")),
                stringOrUnknown(destination.get("server")),
                stringOrUnknown(destination.get("namespace")),
                stringOrUnknown(sync.get("status")),
                stringOrNull(sync.get("revision")),
                stringOrUnknown(health.get("status")),
                stringOrUnknown(operationState.get("phase")),
                stringOrNull(operationState.get("message")),
                stringOrNull(status.get("reconciledAt")),
                stringList(summary.get("images")),
                resources(status.get("resources")),
                conditions(status.get("conditions"))
        );
    }

    private static List<ArgoCdApplicationResource> resources(Object value) {
        if (!(value instanceof List<?> resourceList)) {
            return List.of();
        }

        return resourceList.stream()
                .filter(Map.class::isInstance)
                .map(ArgoCdApplicationDetailMapper::uncheckedMap)
                .map(resource -> new ArgoCdApplicationResource(
                        stringOrUnknown(resource.get("kind")),
                        stringOrUnknown(resource.get("name")),
                        stringOrNull(resource.get("namespace")),
                        stringOrUnknown(resource.get("status")),
                        stringOrUnknown(asMap(resource.get("health")).get("status")),
                        stringOrNull(resource.get("hook")),
                        stringOrNull(resource.get("syncWave"))
                ))
                .toList();
    }

    private static List<ArgoCdApplicationCondition> conditions(Object value) {
        if (!(value instanceof List<?> conditionList)) {
            return List.of();
        }

        return conditionList.stream()
                .filter(Map.class::isInstance)
                .map(ArgoCdApplicationDetailMapper::uncheckedMap)
                .map(condition -> new ArgoCdApplicationCondition(
                        stringOrUnknown(condition.get("type")),
                        stringOrUnknown(condition.get("message")),
                        stringOrNull(condition.get("lastTransitionTime"))
                ))
                .toList();
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> values) {
            return values.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> uncheckedMap(Object value) {
        return (Map<String, Object>) value;
    }

    private static String stringOrUnknown(Object value) {
        String result = stringOrNull(value);
        return result == null || result.isBlank() ? "Unknown" : result;
    }

    private static String stringOrNull(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
