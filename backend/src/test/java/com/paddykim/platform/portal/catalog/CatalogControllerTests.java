package com.paddykim.platform.portal.catalog;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
                .andExpect(jsonPath("$.environments[0].components[1].name", is("platform-mariadb")))
                .andExpect(jsonPath("$.environments[0].components[2].name", is("platform-web")));
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
}
