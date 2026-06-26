package com.paddykim.platform.portal.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "application_manifest_mappings")
public class ApplicationManifestMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_component_id", nullable = false, unique = true)
    private ApplicationComponent component;

    @Column(name = "manifest_repository_url", nullable = false)
    private String manifestRepositoryUrl;

    @Column(name = "manifest_branch", nullable = false)
    private String manifestBranch;

    @Column(name = "values_path", nullable = false)
    private String valuesPath;

    @Column(name = "image_tag_key", nullable = false)
    private String imageTagKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApplicationManifestMapping() {
    }

    public ApplicationManifestMapping(
            ApplicationComponent component,
            String manifestRepositoryUrl,
            String manifestBranch,
            String valuesPath,
            String imageTagKey
    ) {
        this.component = component;
        this.manifestRepositoryUrl = manifestRepositoryUrl;
        this.manifestBranch = manifestBranch;
        this.valuesPath = valuesPath;
        this.imageTagKey = imageTagKey;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(
            String manifestRepositoryUrl,
            String manifestBranch,
            String valuesPath,
            String imageTagKey
    ) {
        this.manifestRepositoryUrl = manifestRepositoryUrl;
        this.manifestBranch = manifestBranch;
        this.valuesPath = valuesPath;
        this.imageTagKey = imageTagKey;
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getManifestRepositoryUrl() {
        return manifestRepositoryUrl;
    }

    public String getManifestBranch() {
        return manifestBranch;
    }

    public String getValuesPath() {
        return valuesPath;
    }

    public String getImageTagKey() {
        return imageTagKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
