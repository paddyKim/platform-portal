package com.paddykim.platform.portal.source;

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
@Table(name = "build_execution_histories")
public class BuildExecutionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_repository_id", nullable = false)
    private SourceRepository sourceRepository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "build_profile_id", nullable = false)
    private BuildProfile buildProfile;

    @Column(name = "external_execution_id")
    private Long externalExecutionId;

    @Column(name = "portal_request_id")
    private Long portalRequestId;

    @Enumerated(EnumType.STRING)
    @Column(name = "runner_type", nullable = false)
    private BuildProfileCiTool runnerType;

    @Column(nullable = false)
    private String branch;

    @Column(name = "requested_by", nullable = false)
    private String requestedBy;

    @Column(name = "requested_value", nullable = false)
    private String requestedValue;

    @Column(nullable = false)
    private String status;

    @Column(name = "status_message", columnDefinition = "TEXT")
    private String statusMessage;

    @Column(name = "clone_status")
    private String cloneStatus;

    @Column(name = "clone_message", columnDefinition = "TEXT")
    private String cloneMessage;

    @Column(name = "checkout_path", columnDefinition = "TEXT")
    private String checkoutPath;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "log_summary", columnDefinition = "TEXT")
    private String logSummary;

    @Column(name = "external_run_id")
    private String externalRunId;

    @Column(name = "external_run_url", columnDefinition = "TEXT")
    private String externalRunUrl;

    @Column(name = "image_repository")
    private String imageRepository;

    @Column(name = "image_tag")
    private String imageTag;

    @Column(name = "image_digest")
    private String imageDigest;

    @Column(name = "image_reference")
    private String imageReference;

    @Column(name = "manifest_update_status")
    private String manifestUpdateStatus;

    @Column(name = "manifest_update_message", columnDefinition = "TEXT")
    private String manifestUpdateMessage;

    @Column(name = "manifest_changed_file_path", columnDefinition = "TEXT")
    private String manifestChangedFilePath;

    @Column(name = "manifest_updated_at")
    private Instant manifestUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BuildExecutionHistory() {
    }

    public BuildExecutionHistory(
            SourceRepository sourceRepository,
            BuildProfile buildProfile,
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
            String imageRepository,
            String imageTag,
            String imageDigest,
            String imageReference
    ) {
        this.sourceRepository = sourceRepository;
        this.buildProfile = buildProfile;
        this.externalExecutionId = externalExecutionId;
        this.portalRequestId = portalRequestId;
        this.runnerType = runnerType;
        this.branch = branch;
        this.requestedBy = requestedBy;
        this.requestedValue = requestedValue;
        this.status = status;
        this.statusMessage = statusMessage;
        this.cloneStatus = cloneStatus;
        this.cloneMessage = cloneMessage;
        this.checkoutPath = checkoutPath;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.exitCode = exitCode;
        this.logSummary = logSummary;
        this.externalRunId = externalRunId;
        this.externalRunUrl = externalRunUrl;
        this.imageRepository = imageRepository;
        this.imageTag = imageTag;
        this.imageDigest = imageDigest;
        this.imageReference = imageReference;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void recordManifestUpdate(String status, String message, String changedFilePath, Instant updatedAt) {
        this.manifestUpdateStatus = status;
        this.manifestUpdateMessage = message;
        this.manifestChangedFilePath = changedFilePath;
        this.manifestUpdatedAt = updatedAt;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public SourceRepository getSourceRepository() {
        return sourceRepository;
    }

    public BuildProfile getBuildProfile() {
        return buildProfile;
    }

    public Long getExternalExecutionId() {
        return externalExecutionId;
    }

    public Long getPortalRequestId() {
        return portalRequestId;
    }

    public BuildProfileCiTool getRunnerType() {
        return runnerType;
    }

    public String getBranch() {
        return branch;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public String getRequestedValue() {
        return requestedValue;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getCloneStatus() {
        return cloneStatus;
    }

    public String getCloneMessage() {
        return cloneMessage;
    }

    public String getCheckoutPath() {
        return checkoutPath;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getLogSummary() {
        return logSummary;
    }

    public String getExternalRunId() {
        return externalRunId;
    }

    public String getExternalRunUrl() {
        return externalRunUrl;
    }

    public String getImageRepository() {
        return imageRepository;
    }

    public String getImageTag() {
        return imageTag;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public String getImageReference() {
        return imageReference;
    }

    public String getManifestUpdateStatus() {
        return manifestUpdateStatus;
    }

    public String getManifestUpdateMessage() {
        return manifestUpdateMessage;
    }

    public String getManifestChangedFilePath() {
        return manifestChangedFilePath;
    }

    public Instant getManifestUpdatedAt() {
        return manifestUpdatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
