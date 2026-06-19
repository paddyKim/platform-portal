package com.paddykim.platform.portal.cicd;

public class CicdRequestNotFoundException extends RuntimeException {

    public CicdRequestNotFoundException(Long id) {
        super("CI/CD request not found: " + id);
    }
}
