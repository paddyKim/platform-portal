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
@Table(name = "build_profiles")
public class BuildProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_repository_id", nullable = false)
    private SourceRepository sourceRepository;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "ci_tool", nullable = false)
    private BuildProfileCiTool ciTool;

    @Column(name = "working_directory", nullable = false)
    private String workingDirectory;

    @Column(nullable = false, length = 12000)
    private String script;

    @Column(nullable = false)
    private String description;

    @Column(name = "target_application_id")
    private Long targetApplicationId;

    @Column(name = "target_environment_id")
    private Long targetEnvironmentId;

    @Column(name = "target_component_id")
    private Long targetComponentId;

    @Column(name = "target_application_name")
    private String targetApplicationName;

    @Column(name = "target_environment")
    private String targetEnvironment;

    @Column(name = "target_component_name")
    private String targetComponentName;

    @Column(name = "target_image_repository")
    private String targetImageRepository;

    @Column(name = "target_helm_values_path")
    private String targetHelmValuesPath;

    @Column(name = "target_argocd_application_name")
    private String targetArgocdApplicationName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected BuildProfile() {
    }

    public BuildProfile(
            SourceRepository sourceRepository,
            String name,
            BuildProfileCiTool ciTool,
            String workingDirectory,
            String script,
            String description,
            BuildProfileTarget target
    ) {
        this.sourceRepository = sourceRepository;
        this.name = name;
        this.ciTool = ciTool;
        this.workingDirectory = workingDirectory;
        this.script = script;
        this.description = description;
        applyTarget(target);
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public BuildProfile(
            SourceRepository sourceRepository,
            String name,
            BuildProfileCiTool ciTool,
            String workingDirectory,
            String script,
            String description
    ) {
        this(sourceRepository, name, ciTool, workingDirectory, script, description, null);
    }

    public void update(
            String name,
            BuildProfileCiTool ciTool,
            String workingDirectory,
            String script,
            String description,
            BuildProfileTarget target
    ) {
        this.name = name;
        this.ciTool = ciTool;
        this.workingDirectory = workingDirectory;
        this.script = script;
        this.description = description;
        applyTarget(target);
        this.updatedAt = Instant.now();
    }

    public void update(
            String name,
            BuildProfileCiTool ciTool,
            String workingDirectory,
            String script,
            String description
    ) {
        update(name, ciTool, workingDirectory, script, description, null);
    }

    private void applyTarget(BuildProfileTarget target) {
        if (target == null) {
            return;
        }

        this.targetApplicationId = target.applicationId();
        this.targetEnvironmentId = target.environmentId();
        this.targetComponentId = target.componentId();
        this.targetApplicationName = target.applicationName();
        this.targetEnvironment = target.environment();
        this.targetComponentName = target.componentName();
        this.targetImageRepository = target.imageRepository();
        this.targetHelmValuesPath = target.helmValuesPath();
        this.targetArgocdApplicationName = target.argocdApplicationName();
    }

    public Long getId() {
        return id;
    }

    public SourceRepository getSourceRepository() {
        return sourceRepository;
    }

    public String getName() {
        return name;
    }

    public BuildProfileCiTool getCiTool() {
        return ciTool;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public String getScript() {
        return script;
    }

    public String getDescription() {
        return description;
    }

    public Long getTargetApplicationId() {
        return targetApplicationId;
    }

    public Long getTargetEnvironmentId() {
        return targetEnvironmentId;
    }

    public Long getTargetComponentId() {
        return targetComponentId;
    }

    public String getTargetApplicationName() {
        return targetApplicationName;
    }

    public String getTargetEnvironment() {
        return targetEnvironment;
    }

    public String getTargetComponentName() {
        return targetComponentName;
    }

    public String getTargetImageRepository() {
        return targetImageRepository;
    }

    public String getTargetHelmValuesPath() {
        return targetHelmValuesPath;
    }

    public String getTargetArgocdApplicationName() {
        return targetArgocdApplicationName;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
