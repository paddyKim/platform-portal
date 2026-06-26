package com.paddykim.platform.portal.catalog;

import com.jayway.jsonpath.JsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CatalogControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    void listsSeededApplications() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("platform-app")))
                .andExpect(jsonPath("$[0].owner", is("platform-team")))
                .andExpect(jsonPath("$[0].environments", hasSize(1)))
                .andExpect(jsonPath("$[0].environments[0].environment", is("dev")))
                .andExpect(jsonPath("$[0].environments[0].components", hasSize(0)));
    }

    @Test
    void returnsApplicationDetailWithEnvironmentAndComponents() throws Exception {
        Long applicationId = applicationRepository.findAll().get(0).getId();

        mockMvc.perform(get("/api/applications/{id}", applicationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("platform-app")))
                .andExpect(jsonPath("$.repositoryUrl", is("https://github.com/paddyKim/platform-app")))
                .andExpect(jsonPath("$.environments[0].environment", is("dev")))
                .andExpect(jsonPath("$.environments[0].namespace", is("dev")))
                .andExpect(jsonPath("$.environments[0].argocdApplicationName", is("platform-dev")))
                .andExpect(jsonPath("$.environments[0].helmValuesPath", is("platform-deploy/environments/dev/values.yaml")))
                .andExpect(jsonPath("$.environments[0].components", hasSize(3)))
                .andExpect(jsonPath("$.environments[0].components[0].name", is("platform-api")))
                .andExpect(jsonPath("$.environments[0].components[0].manifestMapping").isEmpty())
                .andExpect(jsonPath("$.environments[0].components[1].name", is("platform-mariadb")))
                .andExpect(jsonPath("$.environments[0].components[2].name", is("platform-web")));
    }

    @Test
    void managesApplicationEnvironmentComponentAndManifestMapping() throws Exception {
        String applicationResponse = mockMvc.perform(post("/api/applications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "inventory-app",
                                  "description": "Inventory service",
                                  "owner": "platform-team",
                                  "repositoryUrl": "https://github.com/paddyKim/inventory-app"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("inventory-app")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long applicationId = JsonIds.firstId(applicationResponse);

        String environmentResponse = mockMvc.perform(post("/api/applications/{applicationId}/environments", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "environment": "dev",
                                  "namespace": "inventory-dev",
                                  "argocdApplicationName": "inventory-dev",
                                  "helmValuesPath": "environments/dev/values.yaml",
                                  "serviceUrl": "http://inventory.local"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.environments[0].environment", is("dev")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long environmentId = JsonIds.environmentId(environmentResponse);

        String componentResponse = mockMvc.perform(post(
                        "/api/applications/{applicationId}/environments/{environmentId}/components",
                        applicationId,
                        environmentId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "inventory-api",
                                  "kind": "api",
                                  "deploymentName": "inventory-api",
                                  "serviceName": "inventory-api",
                                  "imageRepository": "ghcr.io/paddykim/inventory-api"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.environments[0].components[0].name", is("inventory-api")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long componentId = JsonIds.componentId(componentResponse);

        mockMvc.perform(post(
                        "/api/applications/{applicationId}/environments/{environmentId}/components/{componentId}/manifest-mapping",
                        applicationId,
                        environmentId,
                        componentId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "manifestRepositoryUrl": "https://github.com/paddyKim/platform-deploy.git",
                                  "manifestBranch": "main",
                                  "valuesPath": "environments/dev/values.yaml",
                                  "imageTagKey": "api.image.tag"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.environments[0].components[0].manifestMapping.manifestBranch", is("main")))
                .andExpect(jsonPath("$.environments[0].components[0].manifestMapping.imageTagKey", is("api.image.tag")));

        mockMvc.perform(put("/api/applications/{applicationId}", applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "inventory-app",
                                  "description": "Updated inventory service",
                                  "owner": "application-team",
                                  "repositoryUrl": "https://github.com/paddyKim/inventory-app"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Updated inventory service")))
                .andExpect(jsonPath("$.owner", is("application-team")));

        mockMvc.perform(put(
                        "/api/applications/{applicationId}/environments/{environmentId}/components/{componentId}/manifest-mapping",
                        applicationId,
                        environmentId,
                        componentId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "manifestRepositoryUrl": "https://github.com/paddyKim/platform-deploy.git",
                                  "manifestBranch": "release/dev",
                                  "valuesPath": "environments/dev/values.yaml",
                                  "imageTagKey": "inventory.image.tag"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environments[0].components[0].manifestMapping.manifestBranch", is("release/dev")))
                .andExpect(jsonPath("$.environments[0].components[0].manifestMapping.imageTagKey", is("inventory.image.tag")));

        mockMvc.perform(delete(
                        "/api/applications/{applicationId}/environments/{environmentId}/components/{componentId}/manifest-mapping",
                        applicationId,
                        environmentId,
                        componentId
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environments[0].components[0].manifestMapping").isEmpty());

        mockMvc.perform(delete("/api/applications/{applicationId}", applicationId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/applications/{id}", applicationId))
                .andExpect(status().isNotFound());
    }

    @Test
    void returnsNotFoundForUnknownApplication() throws Exception {
        mockMvc.perform(get("/api/applications/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Application not found: 9999")));
    }

    @Test
    void allowsPortalFrontendOrigin() throws Exception {
        mockMvc.perform(get("/api/applications")
                        .header("Origin", "http://localhost:3001"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3001"));
    }

    private static class JsonIds {

        private static Long firstId(String response) {
            return JsonPath.parse(response).read("$.id", Long.class);
        }

        private static Long environmentId(String response) {
            return JsonPath.parse(response).read("$.environments[0].id", Long.class);
        }

        private static Long componentId(String response) {
            return JsonPath.parse(response).read("$.environments[0].components[0].id", Long.class);
        }
    }
}
