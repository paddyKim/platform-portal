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
        Long targetApplicationId,
        Long targetEnvironmentId,
        Long targetComponentId,
        String targetApplicationName,
        String targetEnvironment,
        String targetComponentName,
        String targetImageRepository,
        String targetHelmValuesPath,
        String targetArgocdApplicationName,
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
                buildProfile.getTargetApplicationId(),
                buildProfile.getTargetEnvironmentId(),
                buildProfile.getTargetComponentId(),
                buildProfile.getTargetApplicationName(),
                buildProfile.getTargetEnvironment(),
                buildProfile.getTargetComponentName(),
                buildProfile.getTargetImageRepository(),
                buildProfile.getTargetHelmValuesPath(),
                buildProfile.getTargetArgocdApplicationName(),
                buildProfile.getCreatedAt(),
                buildProfile.getUpdatedAt()
        );
    }
}
