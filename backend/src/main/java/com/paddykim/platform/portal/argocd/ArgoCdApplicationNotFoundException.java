package com.paddykim.platform.portal.argocd;

public class ArgoCdApplicationNotFoundException extends RuntimeException {

    public ArgoCdApplicationNotFoundException(String applicationName) {
        super("ArgoCD application not found: " + applicationName);
    }
}
