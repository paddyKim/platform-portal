package com.paddykim.platform.portal.runtime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.paddykim.platform.portal.catalog.ApplicationComponent;
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
class RuntimeStatusControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeKubernetesRuntimeClient fakeClient;

    @BeforeEach
    void setUp() {
        fakeClient.unavailable = false;
    }

    @Test
    void returnsAvailableRuntimeStatus() throws Exception {
        mockMvc.perform(get("/api/applications/{id}/environments/{environment}/runtime", 1, "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationName", is("platform-app")))
                .andExpect(jsonPath("$.environment", is("dev")))
                .andExpect(jsonPath("$.namespace", is("dev")))
                .andExpect(jsonPath("$.connectionStatus", is("AVAILABLE")))
                .andExpect(jsonPath("$.summary.desiredReplicas", is(3)))
                .andExpect(jsonPath("$.summary.readyReplicas", is(3)))
                .andExpect(jsonPath("$.summary.availableReplicas", is(3)))
                .andExpect(jsonPath("$.summary.warningEvents", is(1)))
                .andExpect(jsonPath("$.components", hasSize(3)))
                .andExpect(jsonPath("$.components[0].componentName", is("platform-api")))
                .andExpect(jsonPath("$.components[0].status", is("WARNING")))
                .andExpect(jsonPath("$.components[0].pods", hasSize(1)))
                .andExpect(jsonPath("$.components[0].recentEvents", hasSize(1)));
    }

    @Test
    void returnsUnavailableWhenKubernetesRuntimeCannotBeRead() throws Exception {
        fakeClient.unavailable = true;

        mockMvc.perform(get("/api/applications/{id}/environments/{environment}/runtime", 1, "dev"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionStatus", is("UNAVAILABLE")))
                .andExpect(jsonPath("$.components", hasSize(0)))
                .andExpect(jsonPath("$.message", is("Unable to read Kubernetes runtime status")));
    }

    @Test
    void returnsNotFoundForUnknownEnvironment() throws Exception {
        mockMvc.perform(get("/api/applications/{id}/environments/{environment}/runtime", 1, "prd"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("Environment not found")));
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        FakeKubernetesRuntimeClient fakeKubernetesRuntimeClient() {
            return new FakeKubernetesRuntimeClient();
        }
    }

    static class FakeKubernetesRuntimeClient implements KubernetesRuntimeClient {

        private boolean unavailable;

        @Override
        public KubernetesRuntimeSnapshot getRuntime(String namespace, List<ApplicationComponent> components) {
            if (unavailable) {
                throw new KubernetesRuntimeUnavailableException("Unable to read Kubernetes runtime status");
            }

            return new KubernetesRuntimeSnapshot(components.stream()
                    .map(component -> snapshot(component, component.getName().equals("platform-api")))
                    .toList());
        }

        private RuntimeComponentSnapshot snapshot(ApplicationComponent component, boolean warning) {
            return new RuntimeComponentSnapshot(
                    component.getId(),
                    component.getName(),
                    component.getKind(),
                    warning ? "WARNING" : "READY",
                    component.getDeploymentName(),
                    1,
                    1,
                    1,
                    1,
                    component.getServiceName(),
                    "ClusterIP",
                    "10.43.0.1",
                    List.of("http:8080/TCP"),
                    List.of(component.getImageRepository() + ":1fd847c"),
                    List.of(new RuntimePodSnapshot(
                            component.getDeploymentName() + "-abc123",
                            "Running",
                            1,
                            1,
                            warning ? 1 : 0,
                            "10.42.0.10",
                            "colima",
                            "2026-06-19T00:00:00Z"
                    )),
                    warning
                            ? List.of(new RuntimeEventSnapshot(
                                    "Warning",
                                    "Unhealthy",
                                    "Pod",
                                    component.getDeploymentName() + "-abc123",
                                    "Readiness probe failed",
                                    "2026-06-19T00:01:00Z"
                            ))
                            : List.of(),
                    warning ? "Recent warning events detected" : null
            );
        }
    }
}
