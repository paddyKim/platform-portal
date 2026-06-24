package com.paddykim.platform.portal.source;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "source_repositories")
public class SourceRepository {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    private SourceRepositoryProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility")
    private SourceRepositoryVisibility visibility;

    @Column(nullable = false, unique = true)
    private String repositoryUrl;

    @Column(name = "api_base_url")
    private String apiBaseUrl;

    @Column(name = "account_name")
    private String accountName;

    @Column(name = "access_token", length = 2048)
    private String accessToken;

    @Column(nullable = false)
    private String defaultBranch;

    @Column(nullable = false)
    private String description;

    @Column(name = "clone_count")
    private Long cloneCount;

    @Column(name = "build_count")
    private Long buildCount;

    @Column(name = "last_cloned_at")
    private Instant lastClonedAt;

    @Column(name = "last_built_at")
    private Instant lastBuiltAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SourceRepository() {
    }

    public SourceRepository(String name, String repositoryUrl, String defaultBranch, String description) {
        this(
                name,
                SourceRepositoryProvider.GITHUB,
                SourceRepositoryVisibility.PUBLIC,
                repositoryUrl,
                "https://api.github.com",
                "",
                "",
                defaultBranch,
                description
        );
    }

    public SourceRepository(
            String name,
            SourceRepositoryProvider provider,
            SourceRepositoryVisibility visibility,
            String repositoryUrl,
            String apiBaseUrl,
            String accountName,
            String accessToken,
            String defaultBranch,
            String description
    ) {
        this.name = name;
        this.provider = provider;
        this.visibility = visibility;
        this.repositoryUrl = repositoryUrl;
        this.apiBaseUrl = apiBaseUrl;
        this.accountName = accountName;
        this.accessToken = accessToken;
        this.defaultBranch = defaultBranch;
        this.description = description;
        this.cloneCount = 0L;
        this.buildCount = 0L;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SourceRepositoryProvider getProvider() {
        return provider;
    }

    public SourceRepositoryVisibility getVisibility() {
        return visibility;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public String getAccountName() {
        return accountName;
    }

    public boolean hasAccessToken() {
        return accessToken != null && !accessToken.isBlank();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getDefaultBranch() {
        return defaultBranch;
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

    public long getCloneCount() {
        return cloneCount == null ? 0 : cloneCount;
    }

    public long getBuildCount() {
        return buildCount == null ? 0 : buildCount;
    }

    public Instant getLastClonedAt() {
        return lastClonedAt;
    }

    public Instant getLastBuiltAt() {
        return lastBuiltAt;
    }

    public void markCloned(Instant clonedAt) {
        this.cloneCount = getCloneCount() + 1;
        this.lastClonedAt = clonedAt;
        this.updatedAt = clonedAt;
    }

    public void markBuilt(Instant builtAt) {
        this.buildCount = getBuildCount() + 1;
        this.lastBuiltAt = builtAt;
        this.updatedAt = builtAt;
    }
}
