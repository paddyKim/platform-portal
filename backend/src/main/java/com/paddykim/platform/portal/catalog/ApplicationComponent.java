package com.paddykim.platform.portal.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "application_components")
public class ApplicationComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_environment_id", nullable = false)
    private ApplicationEnvironment applicationEnvironment;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String kind;

    @Column(name = "deployment_name", nullable = false)
    private String deploymentName;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(name = "image_repository", nullable = false)
    private String imageRepository;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApplicationComponent() {
    }

    public ApplicationComponent(
            String name,
            String kind,
            String deploymentName,
            String serviceName,
            String imageRepository
    ) {
        this.name = name;
        this.kind = kind;
        this.deploymentName = deploymentName;
        this.serviceName = serviceName;
        this.imageRepository = imageRepository;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    void setApplicationEnvironment(ApplicationEnvironment applicationEnvironment) {
        this.applicationEnvironment = applicationEnvironment;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getImageRepository() {
        return imageRepository;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
