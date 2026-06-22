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
                .andExpect(jsonPath("$[?(@.name == 'platform-deploy')]", hasSize(1)));
    }

    @Test
    void createsSourceRepository() throws Exception {
        mockMvc.perform(post("/api/source-repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "sample-service",
                                  "repositoryUrl": "https://github.com/paddyKim/sample-service",
                                  "defaultBranch": "main",
                                  "description": "Sample service source repository"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("sample-service")))
                .andExpect(jsonPath("$.repositoryUrl", is("https://github.com/paddyKim/sample-service")))
                .andExpect(jsonPath("$.defaultBranch", is("main")));
    }

    @Test
    void rejectsDuplicateSourceRepository() throws Exception {
        mockMvc.perform(post("/api/source-repositories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "platform-app",
                                  "repositoryUrl": "https://github.com/paddyKim/platform-app",
                                  "defaultBranch": "main",
                                  "description": "Duplicate source repository"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already registered")));
    }
}
