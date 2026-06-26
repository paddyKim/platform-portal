package com.paddykim.platform.portal.source;

import java.time.Instant;

public record PlatformCicdExecutionResponse(
        Long executionId,
        Long portalRequestId,
        String status,
        String statusMessage,
        String cloneStatus,
        String cloneMessage,
        String branch,
        String checkoutPath,
        Integer exitCode,
        String logSummary,
        String changedFilePath,
        String imageRepository,
        String imageTag,
        String imageDigest,
        String imageReference,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
) {
}
