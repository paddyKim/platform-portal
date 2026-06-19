package com.paddykim.platform.portal.runtime;

import java.util.List;

public record KubernetesRuntimeSnapshot(
        List<RuntimeComponentSnapshot> components
) {
}
