package com.paddykim.platform.portal.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "application_environments")
public class ApplicationEnvironment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Column(nullable = false)
    private String environment;

    @Column(nullable = false)
    private String namespace;

    @Column(name = "argocd_application_name", nullable = false)
    private String argocdApplicationName;

    @Column(name = "helm_values_path", nullable = false)
    private String helmValuesPath;

    @Column(name = "service_url", nullable = false)
    private String serviceUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "applicationEnvironment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationComponent> components = new ArrayList<>();

    protected ApplicationEnvironment() {
    }

    public ApplicationEnvironment(
            String environment,
            String namespace,
            String argocdApplicationName,
            String helmValuesPath,
            String serviceUrl
    ) {
        this.environment = environment;
        this.namespace = namespace;
        this.argocdApplicationName = argocdApplicationName;
        this.helmValuesPath = helmValuesPath;
        this.serviceUrl = serviceUrl;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void addComponent(ApplicationComponent component) {
        components.add(component);
        component.setApplicationEnvironment(this);
        touch();
    }

    void setApplication(Application application) {
        this.application = application;
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getArgocdApplicationName() {
        return argocdApplicationName;
    }

    public String getHelmValuesPath() {
        return helmValuesPath;
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ApplicationComponent> getComponents() {
        return components;
    }
}
