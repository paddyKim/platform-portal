package com.paddykim.platform.portal.runtime;

public class KubernetesRuntimeUnavailableException extends RuntimeException {

    public KubernetesRuntimeUnavailableException(String message) {
        super(message);
    }

    public KubernetesRuntimeUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
