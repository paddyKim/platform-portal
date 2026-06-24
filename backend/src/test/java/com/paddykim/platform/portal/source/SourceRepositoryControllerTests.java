package com.paddykim.platform.portal.source;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
    private BuildProfileRepository buildProfileRepository;

    @Autowired
    private SourceRepositoryCredentialService credentialService;

    @Autowired
    private RecordingPlatformCicdExecutionClient platformCicdExecutionClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        buildProfileRepository.deleteAll();
        sourceRepositoryRepository.deleteAll();
        platformCicdExecutionClient.reset();
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
                                  "accountName": "paddyKim",
                                  "encryptedAccessToken": "%s"
                                }
                                """.formatted(encryptForNetwork("public-repo-password"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("platform-app")))
                .andExpect(jsonPath("$.provider", is("GITHUB")))
                .andExpect(jsonPath("$.visibility", is("PUBLIC")))
                .andExpect(jsonPath("$.repositoryUrl", is("https://github.com/paddyKim/platform-app")))
                .andExpect(jsonPath("$.apiBaseUrl", is("https://api.github.com")))
                .andExpect(jsonPath("$.accountName", is("paddyKim")))
                .andExpect(jsonPath("$.description", is("")))
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
                                  "accountName": "paddyKim",
                                  "encryptedAccessToken": "%s",
                                  "description": "Private service source repository"
                                }
                                """.formatted(encryptForNetwork("glpat-sample-token"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider", is("GITLAB")))
                .andExpect(jsonPath("$.visibility", is("PRIVATE")))
                .andExpect(jsonPath("$.apiBaseUrl", is("https://gitlab.com/api/v4")))
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

    @Test
    void managesBuildProfilesForSourceRepository() throws Exception {
        SourceRepository repository = sourceRepositoryRepository.save(sourceRepository("platform-app"));

        mockMvc.perform(post("/api/source-repositories/{repositoryId}/build-profiles", repository.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "backend image build",
                                  "ciTool": "SHELL",
                                  "workingDirectory": ".",
                                  "script": "./gradlew test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sourceRepositoryId", is(repository.getId().intValue())))
                .andExpect(jsonPath("$.name", is("backend image build")))
                .andExpect(jsonPath("$.ciTool", is("SHELL")))
                .andExpect(jsonPath("$.workingDirectory", is(".")))
                .andExpect(jsonPath("$.script", is("./gradlew test")))
                .andExpect(jsonPath("$.description", is("")));

        Long profileId = buildProfileRepository.findBySourceRepositoryId(repository.getId()).get(0).getId();

        mockMvc.perform(get("/api/source-repositories/{repositoryId}/build-profiles", repository.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(profileId.intValue())));

        mockMvc.perform(put(
                        "/api/source-repositories/{repositoryId}/build-profiles/{profileId}",
                        repository.getId(),
                        profileId
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "jenkins pipeline draft",
                                  "ciTool": "JENKINS",
                                  "workingDirectory": "backend",
                                  "script": "pipeline { agent any }",
                                  "description": "Draft Jenkins profile"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("jenkins pipeline draft")))
                .andExpect(jsonPath("$.ciTool", is("JENKINS")))
                .andExpect(jsonPath("$.workingDirectory", is("backend")));

        mockMvc.perform(delete(
                        "/api/source-repositories/{repositoryId}/build-profiles/{profileId}",
                        repository.getId(),
                        profileId
                ))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/source-repositories/{repositoryId}/build-profiles", repository.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void preparesBuildProfileRunPayload() throws Exception {
        SourceRepository repository = sourceRepositoryRepository.save(sourceRepository("platform-app"));
        BuildProfile buildProfile = buildProfileRepository.save(new BuildProfile(
                repository,
                "backend image build",
                BuildProfileCiTool.SHELL,
                ".",
                "./gradlew test",
                "Build backend image"
        ));

        mockMvc.perform(post(
                        "/api/source-repositories/{repositoryId}/build-profiles/{profileId}/run",
                        repository.getId(),
                        buildProfile.getId()
                )
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestedBy": "platform-operator",
                                  "imageTag": "day21-test",
                                  "branch": "main"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceRepositoryId", is(repository.getId().intValue())))
                .andExpect(jsonPath("$.buildProfileId", is(buildProfile.getId().intValue())))
                .andExpect(jsonPath("$.repositoryName", is("platform-app")))
                .andExpect(jsonPath("$.ciTool", is("SHELL")))
                .andExpect(jsonPath("$.requestedBy", is("platform-operator")))
                .andExpect(jsonPath("$.imageTag", is("day21-test")))
                .andExpect(jsonPath("$.branch", is("main")))
                .andExpect(jsonPath("$.dispatchTarget", is("platform-cicd-http")))
                .andExpect(jsonPath("$.executionId", is(1001)))
                .andExpect(jsonPath("$.status", is("SUCCEEDED")))
                .andExpect(jsonPath("$.statusMessage", is("Shell script completed with exit code 0")))
                .andExpect(jsonPath("$.cloneStatus", is("SUCCEEDED")))
                .andExpect(jsonPath("$.cloneMessage", is("Git clone completed for branch main")))
                .andExpect(jsonPath("$.checkoutPath", containsString("execution-1001")))
                .andExpect(jsonPath("$.exitCode", is(0)))
                .andExpect(jsonPath("$.logSummary", containsString("fake shell execution")));

        org.assertj.core.api.Assertions.assertThat(platformCicdExecutionClient.lastRequest.branch()).isEqualTo("main");
        org.assertj.core.api.Assertions.assertThat(platformCicdExecutionClient.lastRequest.accountName()).isEqualTo("paddyKim");
        org.assertj.core.api.Assertions.assertThat(platformCicdExecutionClient.lastRequest.credential()).isEqualTo("public-repo-password");

        SourceRepository updated = sourceRepositoryRepository.findById(repository.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(updated.getCloneCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(updated.getBuildCount()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(updated.getLastClonedAt()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(updated.getLastBuiltAt()).isNotNull();
    }

    @Test
    void rejectsBuildProfileOutsideRepositoryWorkspace() throws Exception {
        SourceRepository repository = sourceRepositoryRepository.save(sourceRepository("platform-app"));

        mockMvc.perform(post("/api/source-repositories/{repositoryId}/build-profiles", repository.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "unsafe build",
                                  "ciTool": "SHELL",
                                  "workingDirectory": "../platform-deploy",
                                  "script": "./gradlew test",
                                  "description": "Unsafe build profile"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("workingDirectory")));
    }

    @Test
    void deletesBuildProfilesWithSourceRepository() throws Exception {
        SourceRepository repository = sourceRepositoryRepository.save(sourceRepository("delete-with-profile"));
        buildProfileRepository.save(new BuildProfile(
                repository,
                "backend image build",
                BuildProfileCiTool.SHELL,
                ".",
                "./gradlew test",
                "Build backend image"
        ));

        mockMvc.perform(delete("/api/source-repositories/{id}", repository.getId()))
                .andExpect(status().isNoContent());

        org.assertj.core.api.Assertions.assertThat(buildProfileRepository.findAll()).isEmpty();
    }

    private String encryptForNetwork(String plainText) {
        return credentialService.encryptForNetwork(plainText);
    }

    private SourceRepository sourceRepository(String name) {
        return new SourceRepository(
                name,
                SourceRepositoryProvider.GITHUB,
                SourceRepositoryVisibility.PUBLIC,
                "https://github.com/paddyKim/%s".formatted(name),
                "https://api.github.com",
                "paddyKim",
                credentialService.encryptForStorage("public-repo-password"),
                "",
                "Source repository for tests"
        );
    }

    @TestConfiguration
    static class TestPlatformCicdExecutionClientConfig {

        @Bean
        @Primary
        RecordingPlatformCicdExecutionClient testPlatformCicdExecutionClient() {
            return new RecordingPlatformCicdExecutionClient();
        }
    }

    static class RecordingPlatformCicdExecutionClient implements PlatformCicdExecutionClient {

        private PlatformCicdExecutionCreateRequest lastRequest;

        @Override
        public PlatformCicdExecutionResponse createExecution(PlatformCicdExecutionCreateRequest request) {
            this.lastRequest = request;
            return new PlatformCicdExecutionResponse(
                    1001L,
                    request.portalRequestId(),
                    "SUCCEEDED",
                    "Shell script completed with exit code 0",
                    "SUCCEEDED",
                    "Git clone completed for branch " + request.branch(),
                    request.branch(),
                    "/tmp/execution-1001/repository",
                    0,
                    "fake shell execution for " + request.repositoryUrl(),
                    java.time.Instant.parse("2026-06-23T00:00:00Z"),
                    java.time.Instant.parse("2026-06-23T00:00:01Z"),
                    java.time.Instant.parse("2026-06-23T00:00:00Z")
            );
        }

        void reset() {
            lastRequest = null;
        }
    }
}
