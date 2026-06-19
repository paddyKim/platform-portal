package com.paddykim.platform.portal.cicd;

public class CicdRequestValidationException extends RuntimeException {

    public CicdRequestValidationException(String message) {
        super(message);
    }
}
