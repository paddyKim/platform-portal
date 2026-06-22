package com.paddykim.platform.portal.source;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SourceRepositoryControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SourceRepositoryRepository sourceRepositoryRepository;

    @Autowired
    private SourceRepositoryCredentialService credentialService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void allowsPortalFrontendPostPreflight() throws Exception {
        mockMvc.perform(options("/api/source-repositories")
                        .header("Origin", "http://localhost:3001")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3001"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
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
                                  "encryptedAccessToken": "%s",
                                  "description": "Public app source repository"
                                }
                                """.formatted(encryptForNetwork("public-repo-password"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("platform-app")))
                .andExpect(jsonPath("$.provider", is("GITHUB")))
                .andExpect(jsonPath("$.visibility", is("PUBLIC")))
                .andExpect(jsonPath("$.repositoryUrl", is("https://github.com/paddyKim/platform-app")))
                .andExpect(jsonPath("$.accountName", is("paddyKim")))
                .andExpect(jsonPath("$.credentialConfigured", is(true)))
                .andExpect(jsonPath("$.cloneCount", is(0)))
                .andExpect(jsonPath("$.buildCount", is(0)));

        String storedToken = jdbcTemplate.queryForObject(
                "select access_token from source_repositories where repository_url = ?",
                String.class,
                "https://github.com/paddyKim/platform-app"
        );
        org.assertj.core.api.Assertions.assertThat(storedToken)
                .isNotBlank()
                .isNotEqualTo("public-repo-password")
                .startsWith("v1:");
        org.assertj.core.api.Assertions.assertThat(credentialService.decryptFromStorage(storedToken))
                .isEqualTo("public-repo-password");
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
                                  "encryptedAccessToken": "%s",
                                  "description": "Private service source repository"
                                }
                                """.formatted(encryptForNetwork("glpat-sample-token"))))
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
                                  "encryptedAccessToken": "",
                                  "description": "Source repository without credential"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("encryptedAccessToken")));
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
                                  "encryptedAccessToken": "%s",
                                  "description": "Duplicate source repository"
                                }
                                """.formatted(encryptForNetwork("public-repo-password"))))
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
                                  "encryptedAccessToken": "%s",
                                  "description": "Duplicate source repository"
                                }
                                """.formatted(encryptForNetwork("public-repo-password"))))
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
                                  "encryptedAccessToken": "%s",
                                  "description": "Repository to delete"
                                }
                                """.formatted(encryptForNetwork("public-repo-password"))))
                .andExpect(status().isCreated());

        Long repositoryId = sourceRepositoryRepository.findAll().get(0).getId();
        mockMvc.perform(delete("/api/source-repositories/{id}", repositoryId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/source-repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private String encryptForNetwork(String plainText) {
        return credentialService.encryptForNetwork(plainText);
    }
}
