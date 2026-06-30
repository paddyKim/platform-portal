package com.paddykim.platform.portal.argocd;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArgoCdApplicationDetailMapperTests {

    @Test
    void mapsApplicationSpecStatusResourcesAndConditions() {
        ArgoCdApplicationDetail detail = ArgoCdApplicationDetailMapper.from(
                "platform-dev",
                "argocd",
                Map.of(
                        "spec", Map.of(
                                "project", "default",
                                "source", Map.of(
                                        "repoURL", "https://github.com/paddyKim/platform-deploy.git",
                                        "path", "environments/dev",
                                        "targetRevision", "main"
                                ),
                                "destination", Map.of(
                                        "server", "https://kubernetes.default.svc",
                                        "namespace", "dev"
                                )
                        ),
                        "status", Map.of(
                                "sync", Map.of("status", "Synced", "revision", "a1b2c3d"),
                                "health", Map.of("status", "Healthy"),
                                "operationState", Map.of("phase", "Succeeded", "message", "sync completed"),
                                "reconciledAt", "2026-06-29T01:00:00Z",
                                "summary", Map.of("images", List.of("example/app:a1b2c3d")),
                                "resources", List.of(Map.of(
                                        "kind", "Deployment",
                                        "name", "platform-api",
                                        "namespace", "dev",
                                        "status", "Synced",
                                        "health", Map.of("status", "Healthy"),
                                        "syncWave", "0"
                                )),
                                "conditions", List.of(Map.of(
                                        "type", "ComparisonError",
                                        "message", "example condition",
                                        "lastTransitionTime", "2026-06-29T01:00:00Z"
                                ))
                        )
                )
        );

        assertThat(detail.name()).isEqualTo("platform-dev");
        assertThat(detail.sourcePath()).isEqualTo("environments/dev");
        assertThat(detail.syncRevision()).isEqualTo("a1b2c3d");
        assertThat(detail.operationMessage()).isEqualTo("sync completed");
        assertThat(detail.images()).containsExactly("example/app:a1b2c3d");
        assertThat(detail.resources()).containsExactly(new ArgoCdApplicationResource(
                "Deployment",
                "platform-api",
                "dev",
                "Synced",
                "Healthy",
                null,
                "0"
        ));
        assertThat(detail.conditions()).hasSize(1);
    }

    @Test
    void usesSafeDefaultsForMissingStatus() {
        ArgoCdApplicationDetail detail = ArgoCdApplicationDetailMapper.from(
                "platform-dev",
                "argocd",
                Map.of()
        );

        assertThat(detail.syncStatus()).isEqualTo("Unknown");
        assertThat(detail.healthStatus()).isEqualTo("Unknown");
        assertThat(detail.resources()).isEmpty();
        assertThat(detail.conditions()).isEmpty();
    }
}
