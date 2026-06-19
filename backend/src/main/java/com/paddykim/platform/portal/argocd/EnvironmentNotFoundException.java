package com.paddykim.platform.portal.argocd;

public class EnvironmentNotFoundException extends RuntimeException {

    public EnvironmentNotFoundException(Long applicationId, String environment) {
        super("Environment not found: application=" + applicationId + ", environment=" + environment);
    }
}
