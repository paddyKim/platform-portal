package com.paddykim.platform.portal.source;

public class BuildProfileNotFoundException extends RuntimeException {

    public BuildProfileNotFoundException(Long sourceRepositoryId, Long buildProfileId) {
        super("Build profile not found for source repository %d: %d".formatted(sourceRepositoryId, buildProfileId));
    }
}
