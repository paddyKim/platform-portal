package com.paddykim.platform.portal.argocd;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ArgoCdStatusControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeArgoCdApplicationStatusClient fakeClient;

    @BeforeEach
    void setUp() {
        fakeClient.unavailable = false;
    }

    @Test
    void returnsAvailableArgoCdStatus() throws Exception {
        mockMvc.perform(get("/api/applications/{id}/environments/{environment}/status", 1, "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationName", is("platform-app")))
                .andExpect(jsonPath("$.environment", is("dev")))
                .andExpect(jsonPath("$.argocdApplicationName", is("platform-dev")))
                .andExpect(jsonPath("$.connectionStatus", is("AVAILABLE")))
                .andExpect(jsonPath("$.syncStatus", is("Synced")))
                .andExpect(jsonPath("$.healthStatus", is("Healthy")))
                .andExpect(jsonPath("$.operationPhase", is("Succeeded")))
                .andExpect(jsonPath("$.reconciledAt", is("2026-06-18T08:09:53Z")))
                .andExpect(jsonPath("$.images", hasSize(2)));
    }

    @Test
    void returnsUnavailableWhenArgoCdStatusCannotBeRead() throws Exception {
        fakeClient.unavailable = true;

        mockMvc.perform(get("/api/applications/{id}/environments/{environment}/status", 1, "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionStatus", is("UNAVAILABLE")))
                .andExpect(jsonPath("$.syncStatus", is("Unknown")))
                .andExpect(jsonPath("$.healthStatus", is("Unknown")))
                .andExpect(jsonPath("$.message", is("Unable to read ArgoCD application status")));
    }

    @Test
    void returnsNotFoundForUnknownEnvironment() throws Exception {
        mockMvc.perform(get("/api/applications/{id}/environments/{environment}/status", 1, "prd"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Environment not found")));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FakeArgoCdApplicationStatusClient fakeArgoCdApplicationStatusClient() {
            return new FakeArgoCdApplicationStatusClient();
        }
    }

    static class FakeArgoCdApplicationStatusClient implements ArgoCdApplicationStatusClient {

        private boolean unavailable;

        @Override
        public ArgoCdApplicationSnapshot getApplicationStatus(String applicationName) {
            if (unavailable) {
                throw new ArgoCdStatusUnavailableException("Unable to read ArgoCD application status");
            }

            return new ArgoCdApplicationSnapshot(
                    "Synced",
                    "Healthy",
                    "Succeeded",
                    "2026-06-18T08:09:53Z",
                    List.of(
                            "ghcr.io/paddykim/platform-api:1fd847c",
                            "ghcr.io/paddykim/platform-web:1fd847c"
                    )
            );
        }
    }
}
