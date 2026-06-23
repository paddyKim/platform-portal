package com.paddykim.platform.portal.source;

import java.time.Instant;

public record BuildProfileResponse(
        Long id,
        Long sourceRepositoryId,
        String name,
        BuildProfileCiTool ciTool,
        String workingDirectory,
        String script,
        String description,
        Instant createdAt,
        Instant updatedAt
) {

    static BuildProfileResponse from(BuildProfile buildProfile) {
        return new BuildProfileResponse(
                buildProfile.getId(),
                buildProfile.getSourceRepository().getId(),
                buildProfile.getName(),
                buildProfile.getCiTool(),
                buildProfile.getWorkingDirectory(),
                buildProfile.getScript(),
                buildProfile.getDescription(),
                buildProfile.getCreatedAt(),
                buildProfile.getUpdatedAt()
        );
    }
}
