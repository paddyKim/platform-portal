package com.paddykim.platform.portal.source;

public class SourceRepositoryNotFoundException extends RuntimeException {

    public SourceRepositoryNotFoundException(Long id) {
        super("Source repository not found: " + id);
    }
}
