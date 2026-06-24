package com.paddykim.platform.portal.source;

import java.time.Instant;

public record BuildExecutionHistoryResponse(
        Long id,
        Long sourceRepositoryId,
        Long buildProfileId,
        String buildProfileName,
        Long externalExecutionId,
        Long portalRequestId,
        BuildProfileCiTool runnerType,
        String branch,
        String requestedBy,
        String requestedValue,
        String status,
        String statusMessage,
        String cloneStatus,
        String cloneMessage,
        String checkoutPath,
        Instant startedAt,
        Instant finishedAt,
        Integer exitCode,
        String logSummary,
        String externalRunId,
        String externalRunUrl,
        Instant createdAt,
        Instant updatedAt
) {

    static BuildExecutionHistoryResponse from(BuildExecutionHistory history) {
        return new BuildExecutionHistoryResponse(
                history.getId(),
                history.getSourceRepository().getId(),
                history.getBuildProfile().getId(),
                history.getBuildProfile().getName(),
                history.getExternalExecutionId(),
                history.getPortalRequestId(),
                history.getRunnerType(),
                history.getBranch(),
                history.getRequestedBy(),
                history.getRequestedValue(),
                history.getStatus(),
                history.getStatusMessage(),
                history.getCloneStatus(),
                history.getCloneMessage(),
                history.getCheckoutPath(),
                history.getStartedAt(),
                history.getFinishedAt(),
                history.getExitCode(),
                history.getLogSummary(),
                history.getExternalRunId(),
                history.getExternalRunUrl(),
                history.getCreatedAt(),
                history.getUpdatedAt()
        );
    }
}
