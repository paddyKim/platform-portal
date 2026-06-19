package com.paddykim.platform.portal.runtime;

import com.paddykim.platform.portal.catalog.ApplicationComponent;
import java.util.List;

public interface KubernetesRuntimeClient {

    KubernetesRuntimeSnapshot getRuntime(String namespace, List<ApplicationComponent> components);
}
