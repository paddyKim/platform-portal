package com.paddykim.platform.portal.argocd;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ArgoCdApplicationCatalogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsApplicationDetail() throws Exception {
        mockMvc.perform(get("/api/argocd/applications/{applicationName}", "platform-dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("platform-dev")))
                .andExpect(jsonPath("$.syncStatus", is("Synced")))
                .andExpect(jsonPath("$.syncRevision", is("a1b2c3d")))
                .andExpect(jsonPath("$.resources", hasSize(1)))
                .andExpect(jsonPath("$.resources[0].kind", is("Deployment")))
                .andExpect(jsonPath("$.conditions", hasSize(1)));
    }

    @Test
    void acceptsSyncRequest() throws Exception {
        mockMvc.perform(post("/api/argocd/applications/{applicationName}/sync", "platform-dev")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prune": true,
                                  "dryRun": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("platform-dev")))
                .andExpect(jsonPath("$.operationPhase", is("Running")));
    }

    @Test
    void rejectsIncompleteSyncRequest() throws Exception {
        mockMvc.perform(post("/api/argocd/applications/{applicationName}/sync", "platform-dev")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prune": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("dryRun must not be null")));
    }

    @Test
    void returnsNotFoundForUnknownApplication() throws Exception {
        mockMvc.perform(get("/api/argocd/applications/{applicationName}", "unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("ArgoCD application not found: unknown")));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        ArgoCdApplicationCatalogClient fakeArgoCdApplicationCatalogClient() {
            return new FakeArgoCdApplicationCatalogClient();
        }
    }

    static class FakeArgoCdApplicationCatalogClient implements ArgoCdApplicationCatalogClient {

        @Override
        public List<ArgoCdApplicationSummary> listApplications() {
            return List.of();
        }

        @Override
        public ArgoCdApplicationSummary createApplication(ArgoCdApplicationCreateRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ArgoCdApplicationDetail getApplication(String applicationName) {
            if ("unknown".equals(applicationName)) {
                throw new ArgoCdApplicationNotFoundException(applicationName);
            }
            return detail("Succeeded");
        }

        @Override
        public ArgoCdApplicationDetail syncApplication(
                String applicationName,
                ArgoCdApplicationSyncRequest request
        ) {
            return detail("Running");
        }

        private ArgoCdApplicationDetail detail(String phase) {
            return new ArgoCdApplicationDetail(
                    "platform-dev",
                    "argocd",
                    "default",
                    "https://github.com/paddyKim/platform-deploy.git",
                    "environments/dev",
                    "main",
                    "https://kubernetes.default.svc",
                    "dev",
                    "Synced",
                    "a1b2c3d",
                    "Healthy",
                    phase,
                    "operation message",
                    "2026-06-29T01:00:00Z",
                    List.of("ghcr.io/paddykim/platform-api:a1b2c3d"),
                    List.of(new ArgoCdApplicationResource(
                            "Deployment",
                            "platform-api",
                            "dev",
                            "Synced",
                            "Healthy",
                            null,
                            "0"
                    )),
                    List.of(new ArgoCdApplicationCondition(
                            "ComparisonError",
                            "example condition",
                            "2026-06-29T01:00:00Z"
                    ))
            );
        }
    }
}
