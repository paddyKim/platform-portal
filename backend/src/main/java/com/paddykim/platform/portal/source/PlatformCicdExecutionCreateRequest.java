package com.paddykim.platform.portal.source;

public record PlatformCicdExecutionCreateRequest(
        Long portalRequestId,
        String applicationName,
        String environment,
        String componentName,
        String requestType,
        String requestedValue,
        String requestedBy,
        Long sourceRepositoryId,
        Long buildProfileId,
        String ciTool,
        String repositoryUrl,
        String workingDirectory,
        String script
) {
}
