package com.paddykim.platform.portal.argocd;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ArgoCdStatusMapperTests {

    @Test
    void mapsArgoCdApplicationStatus() {
        ArgoCdApplicationSnapshot snapshot = ArgoCdStatusMapper.from(Map.of(
                "status", Map.of(
                        "sync", Map.of("status", "Synced"),
                        "health", Map.of("status", "Healthy"),
                        "operationState", Map.of("phase", "Succeeded"),
                        "reconciledAt", "2026-06-18T08:09:53Z",
                        "summary", Map.of("images", List.of(
                                "ghcr.io/paddykim/platform-api:1fd847c",
                                "ghcr.io/paddykim/platform-web:1fd847c"
                        ))
                )
        ));

        assertThat(snapshot.syncStatus()).isEqualTo("Synced");
        assertThat(snapshot.healthStatus()).isEqualTo("Healthy");
        assertThat(snapshot.operationPhase()).isEqualTo("Succeeded");
        assertThat(snapshot.reconciledAt()).isEqualTo("2026-06-18T08:09:53Z");
        assertThat(snapshot.images()).containsExactly(
                "ghcr.io/paddykim/platform-api:1fd847c",
                "ghcr.io/paddykim/platform-web:1fd847c"
        );
    }

    @Test
    void usesUnknownForMissingStatusFields() {
        ArgoCdApplicationSnapshot snapshot = ArgoCdStatusMapper.from(Map.of());

        assertThat(snapshot.syncStatus()).isEqualTo("Unknown");
        assertThat(snapshot.healthStatus()).isEqualTo("Unknown");
        assertThat(snapshot.operationPhase()).isEqualTo("Unknown");
        assertThat(snapshot.reconciledAt()).isNull();
        assertThat(snapshot.images()).isEmpty();
    }
}
