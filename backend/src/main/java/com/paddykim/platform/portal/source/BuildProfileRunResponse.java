package com.paddykim.platform.portal.source;

import java.time.Instant;

public record BuildProfileRunResponse(
        Long sourceRepositoryId,
        Long buildProfileId,
        Long executionId,
        Long portalRequestId,
        String repositoryName,
        String repositoryUrl,
        BuildProfileCiTool ciTool,
        String workingDirectory,
        String requestedBy,
        String imageTag,
        String branch,
        String dispatchTarget,
        String status,
        String statusMessage,
        String cloneStatus,
        String cloneMessage,
        String checkoutPath,
        Integer exitCode,
        String logSummary,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
) {

    static BuildProfileRunResponse from(
            Long sourceRepositoryId,
            Long buildProfileId,
            String repositoryName,
            String repositoryUrl,
            BuildProfileCiTool ciTool,
            String workingDirectory,
            String requestedBy,
            String imageTag,
            String branch,
            String dispatchTarget,
            PlatformCicdExecutionResponse execution
    ) {
        return new BuildProfileRunResponse(
                sourceRepositoryId,
                buildProfileId,
                execution.executionId(),
                execution.portalRequestId(),
                repositoryName,
                repositoryUrl,
                ciTool,
                workingDirectory,
                requestedBy,
                imageTag,
                branch,
                dispatchTarget,
                execution.status(),
                execution.statusMessage(),
                execution.cloneStatus(),
                execution.cloneMessage(),
                execution.checkoutPath(),
                execution.exitCode(),
                execution.logSummary(),
                execution.startedAt(),
                execution.finishedAt(),
                execution.createdAt()
        );
    }
}
