package com.paddykim.platform.portal.source;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
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

    @Autowired
    private SourceRepositoryRepository sourceRepositoryRepository;

    @BeforeEach
    void setUp() {
        sourceRepositoryRepository.deleteAll();
    }

    @Test
    void listsSourceRepositories() throws Exception {
        mockMvc.perform(get("/api/source-repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void createsPublicSourceRepositoryWithCredential() throws Exception {
        mockMvc.perform(post("/api/source-repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "platform-app",
                                  "provider": "GITHUB",
                                  "visibility": "PUBLIC",
                                  "repositoryUrl": "https://github.com/paddyKim/platform-app",
                                  "apiBaseUrl": "https://api.github.com",
                                  "accountName": "paddyKim",
                                  "accessToken": "public-repo-password",
                                  "description": "Public app source repository"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("platform-app")))
                .andExpect(jsonPath("$.provider", is("GITHUB")))
                .andExpect(jsonPath("$.visibility", is("PUBLIC")))
                .andExpect(jsonPath("$.repositoryUrl", is("https://github.com/paddyKim/platform-app")))
                .andExpect(jsonPath("$.accountName", is("paddyKim")))
                .andExpect(jsonPath("$.credentialConfigured", is(true)))
                .andExpect(jsonPath("$.cloneCount", is(0)))
                .andExpect(jsonPath("$.buildCount", is(0)));
    }

    @Test
    void createsPrivateSourceRepositoryWithToken() throws Exception {
        mockMvc.perform(post("/api/source-repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "sample-service",
                                  "provider": "GITLAB",
                                  "visibility": "PRIVATE",
                                  "repositoryUrl": "https://gitlab.com/paddyKim/sample-service",
                                  "apiBaseUrl": "https://gitlab.com/api/v4",
                                  "accountName": "paddyKim",
                                  "accessToken": "glpat-sample-token",
                                  "description": "Private service source repository"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider", is("GITLAB")))
                .andExpect(jsonPath("$.visibility", is("PRIVATE")))
                .andExpect(jsonPath("$.credentialConfigured", is(true)));
    }

    @Test
    void rejectsRepositoryWithoutCredential() throws Exception {
        mockMvc.perform(post("/api/source-repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "private-service",
                                  "provider": "GITHUB",
                                  "visibility": "PRIVATE",
                                  "repositoryUrl": "https://github.com/paddyKim/private-service",
                                  "apiBaseUrl": "https://api.github.com",
                                  "accountName": "paddyKim",
                                  "accessToken": "",
                                  "description": "Source repository without credential"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("accessToken")));
    }

    @Test
    void rejectsDuplicateSourceRepository() throws Exception {
        mockMvc.perform(post("/api/source-repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "platform-app",
                                  "provider": "GITHUB",
                                  "visibility": "PUBLIC",
                                  "repositoryUrl": "https://github.com/paddyKim/platform-app",
                                  "apiBaseUrl": "https://api.github.com",
                                  "accountName": "paddyKim",
                                  "accessToken": "public-repo-password",
                                  "description": "Duplicate source repository"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/source-repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "platform-app-copy",
                                  "provider": "GITHUB",
                                  "visibility": "PUBLIC",
                                  "repositoryUrl": "https://github.com/paddyKim/platform-app",
                                  "apiBaseUrl": "https://api.github.com",
                                  "accountName": "paddyKim",
                                  "accessToken": "public-repo-password",
                                  "description": "Duplicate source repository"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }

    @Test
    void deletesSourceRepository() throws Exception {
        mockMvc.perform(post("/api/source-repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "delete-me",
                                  "provider": "GITHUB",
                                  "visibility": "PUBLIC",
                                  "repositoryUrl": "https://github.com/paddyKim/delete-me",
                                  "apiBaseUrl": "https://api.github.com",
                                  "accountName": "paddyKim",
                                  "accessToken": "public-repo-password",
                                  "description": "Repository to delete"
                                }
                                """))
                .andExpect(status().isCreated());

        Long repositoryId = sourceRepositoryRepository.findAll().get(0).getId();
        mockMvc.perform(delete("/api/source-repositories/{id}", repositoryId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/source-repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
