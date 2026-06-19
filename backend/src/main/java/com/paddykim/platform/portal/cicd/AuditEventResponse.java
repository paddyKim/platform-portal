package com.paddykim.platform.portal.cicd;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        Long cicdRequestId,
        AuditEventType eventType,
        String actor,
        String target,
        String description,
        Instant createdAt
) {

    static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getCicdRequest() == null ? null : event.getCicdRequest().getId(),
                event.getEventType(),
                event.getActor(),
                event.getTarget(),
                event.getDescription(),
                event.getCreatedAt()
        );
    }
}
