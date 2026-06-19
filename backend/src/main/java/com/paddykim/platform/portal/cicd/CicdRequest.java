package com.paddykim.platform.portal.cicd;

import com.paddykim.platform.portal.catalog.Application;
import com.paddykim.platform.portal.catalog.ApplicationComponent;
import com.paddykim.platform.portal.catalog.ApplicationEnvironment;
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
@Table(name = "cicd_requests")
public class CicdRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "environment_id", nullable = false)
    private ApplicationEnvironment environment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id", nullable = false)
    private ApplicationComponent component;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private CicdRequestType requestType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CicdRequestStatus status;

    @Column(name = "requested_value", nullable = false)
    private String requestedValue;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "message_topic", nullable = false)
    private String messageTopic;

    @Column(name = "message_key", nullable = false)
    private String messageKey;

    @Column(name = "status_message")
    private String statusMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CicdRequest() {
    }

    public CicdRequest(
            Application application,
            ApplicationEnvironment environment,
            ApplicationComponent component,
            CicdRequestType requestType,
            String requestedValue,
            String requestedBy,
            String messageTopic,
            String messageKey
    ) {
        this.application = application;
        this.environment = environment;
        this.component = component;
        this.requestType = requestType;
        this.status = CicdRequestStatus.REQUESTED;
        this.requestedValue = requestedValue;
        this.requestedBy = requestedBy;
        this.messageTopic = messageTopic;
        this.messageKey = messageKey;
        this.statusMessage = "Request recorded; waiting for platform-cicd dispatch";
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public Application getApplication() {
        return application;
    }

    public ApplicationEnvironment getEnvironment() {
        return environment;
    }

    public ApplicationComponent getComponent() {
        return component;
    }

    public CicdRequestType getRequestType() {
        return requestType;
    }

    public CicdRequestStatus getStatus() {
        return status;
    }

    public String getRequestedValue() {
        return requestedValue;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getMessageTopic() {
        return messageTopic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
