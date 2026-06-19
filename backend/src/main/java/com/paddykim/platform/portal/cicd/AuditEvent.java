package com.paddykim.platform.portal.cicd;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "audit_events")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cicd_request_id")
    private CicdRequest cicdRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private AuditEventType eventType;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String target;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuditEvent() {
    }

    public AuditEvent(
            CicdRequest cicdRequest,
            AuditEventType eventType,
            String actor,
            String target,
            String description
    ) {
        this.cicdRequest = cicdRequest;
        this.eventType = eventType;
        this.actor = actor;
        this.target = target;
        this.description = description;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CicdRequest getCicdRequest() {
        return cicdRequest;
    }

    public AuditEventType getEventType() {
        return eventType;
    }

    public String getActor() {
        return actor;
    }

    public String getTarget() {
        return target;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
