package com.paddykim.platform.portal.source;

import java.time.Instant;

public record BuildProfileRunResponse(
        Long sourceRepositoryId,
        Long buildProfileId,
        String repositoryName,
        String repositoryUrl,
        BuildProfileCiTool ciTool,
        String workingDirectory,
        String requestedBy,
        String imageTag,
        String dispatchTarget,
        String status,
        String statusMessage,
        Instant createdAt
) {
}
