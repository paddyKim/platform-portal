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
            String description
    ) {
        this.sourceRepository = sourceRepository;
        this.name = name;
        this.ciTool = ciTool;
        this.workingDirectory = workingDirectory;
        this.script = script;
        this.description = description;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(
            String name,
            BuildProfileCiTool ciTool,
            String workingDirectory,
            String script,
            String description
    ) {
        this.name = name;
        this.ciTool = ciTool;
        this.workingDirectory = workingDirectory;
        this.script = script;
        this.description = description;
        this.updatedAt = Instant.now();
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
