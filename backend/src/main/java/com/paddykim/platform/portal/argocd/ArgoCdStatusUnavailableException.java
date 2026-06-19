package com.paddykim.platform.portal.argocd;

public class ArgoCdStatusUnavailableException extends RuntimeException {

    public ArgoCdStatusUnavailableException(String message) {
        super(message);
    }

    public ArgoCdStatusUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
