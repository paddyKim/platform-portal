package com.paddykim.platform.portal.catalog;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String owner;

    @Column(name = "repository_url", nullable = false)
    private String repositoryUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationEnvironment> environments = new ArrayList<>();

    protected Application() {
    }

    public Application(String name, String description, String owner, String repositoryUrl) {
        this.name = name;
        this.description = description;
        this.owner = owner;
        this.repositoryUrl = repositoryUrl;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void addEnvironment(ApplicationEnvironment environment) {
        environments.add(environment);
        environment.setApplication(this);
        touch();
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getOwner() {
        return owner;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ApplicationEnvironment> getEnvironments() {
        return environments;
    }
}
