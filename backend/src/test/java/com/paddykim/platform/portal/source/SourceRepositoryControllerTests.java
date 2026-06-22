package com.paddykim.platform.portal.source;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class SourceRepositoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsSeededSourceRepositories() throws Exception {
        mockMvc.perform(get("/api/source-repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$[?(@.name == 'platform-app')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.name == 'platform-deploy')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.provider == 'GITHUB')]", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void createsSourceRepository() throws Exception {
        mockMvc.perform(post("/api/source-repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "sample-service",
                                  "provider": "GITLAB",
                                  "repositoryUrl": "https://gitlab.com/paddyKim/sample-service",
                                  "apiBaseUrl": "https://gitlab.com/api/v4",
                                  "accountName": "paddyKim",
                                  "accessToken": "glpat-sample-token",
                                  "defaultBranch": "main",
                                  "description": "Sample service source repository"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("sample-service")))
                .andExpect(jsonPath("$.provider", is("GITLAB")))
                .andExpect(jsonPath("$.repositoryUrl", is("https://gitlab.com/paddyKim/sample-service")))
                .andExpect(jsonPath("$.apiBaseUrl", is("https://gitlab.com/api/v4")))
                .andExpect(jsonPath("$.accountName", is("paddyKim")))
                .andExpect(jsonPath("$.credentialConfigured", is(true)))
                .andExpect(jsonPath("$.defaultBranch", is("main")));
    }

    @Test
    void rejectsDuplicateSourceRepository() throws Exception {
        mockMvc.perform(post("/api/source-repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "platform-app",
                                  "provider": "GITHUB",
                                  "repositoryUrl": "https://github.com/paddyKim/platform-app",
                                  "apiBaseUrl": "https://api.github.com",
                                  "accountName": "paddyKim",
                                  "accessToken": "ghp-sample-token",
                                  "defaultBranch": "main",
                                  "description": "Duplicate source repository"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }
}
