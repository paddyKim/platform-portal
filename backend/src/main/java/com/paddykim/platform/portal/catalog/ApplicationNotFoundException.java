package com.paddykim.platform.portal.catalog;

public class ApplicationNotFoundException extends RuntimeException {

    public ApplicationNotFoundException(Long id) {
        super("Application not found: " + id);
    }
}
