package com.paddykim.platform.portal.cicd;

import java.time.Instant;

public record CicdRequestResponse(
        Long id,
        Long applicationId,
        String applicationName,
        String environment,
        Long componentId,
        String componentName,
        CicdRequestType requestType,
        CicdRequestStatus status,
        String requestedValue,
        String requestedBy,
        String messageTopic,
        String messageKey,
        String statusMessage,
        Instant createdAt,
        Instant updatedAt
) {

    static CicdRequestResponse from(CicdRequest request) {
        return new CicdRequestResponse(
                request.getId(),
                request.getApplication().getId(),
                request.getApplication().getName(),
                request.getEnvironment().getEnvironment(),
                request.getComponent().getId(),
                request.getComponent().getName(),
                request.getRequestType(),
                request.getStatus(),
                request.getRequestedValue(),
                request.getRequestedBy(),
                request.getMessageTopic(),
                request.getMessageKey(),
                request.getStatusMessage(),
                request.getCreatedAt(),
                request.getUpdatedAt()
        );
    }
}
