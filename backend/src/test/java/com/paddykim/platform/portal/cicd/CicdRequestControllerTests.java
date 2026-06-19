package com.paddykim.platform.portal.cicd;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
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
class CicdRequestControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CicdRequestRepository cicdRequestRepository;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
        cicdRequestRepository.deleteAll();
    }

    @Test
    void createsCicdRequestAndAuditEvent() throws Exception {
        mockMvc.perform(post("/api/cicd/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationId": 1,
                                  "environment": "dev",
                                  "componentId": 1,
                                  "requestType": "BUILD_IMAGE",
                                  "requestedValue": "main",
                                  "requestedBy": "platform-operator"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.applicationName", is("platform-app")))
                .andExpect(jsonPath("$.environment", is("dev")))
                .andExpect(jsonPath("$.componentName", is("platform-api")))
                .andExpect(jsonPath("$.requestType", is("BUILD_IMAGE")))
                .andExpect(jsonPath("$.status", is("DISPATCHED")))
                .andExpect(jsonPath("$.requestedValue", is("main")))
                .andExpect(jsonPath("$.requestedBy", is("platform-operator")))
                .andExpect(jsonPath("$.dispatchTarget", is("platform-cicd-http")))
                .andExpect(jsonPath("$.messageKey", is("platform-app:dev:platform-api")));

        mockMvc.perform(get("/api/audit-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eventType", is("CICD_REQUEST_CREATED")))
                .andExpect(jsonPath("$[0].actor", is("platform-operator")))
                .andExpect(jsonPath("$[0].target", is("platform-app:dev:platform-api")));
    }

    @Test
    void listsAndReadsCicdRequests() throws Exception {
        mockMvc.perform(post("/api/cicd/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationId": 1,
                                  "environment": "dev",
                                  "componentId": 1,
                                  "requestType": "DEPLOY_IMAGE",
                                  "requestedValue": "1fd847c",
                                  "requestedBy": "platform-operator"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/cicd/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].requestType", is("DEPLOY_IMAGE")));

        Long requestId = cicdRequestRepository.findAll().get(0).getId();
        mockMvc.perform(get("/api/cicd/requests/{id}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(requestId.intValue())))
                .andExpect(jsonPath("$.status", is("DISPATCHED")));
    }

    @Test
    void rejectsUnknownComponent() throws Exception {
        mockMvc.perform(post("/api/cicd/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "applicationId": 1,
                                  "environment": "dev",
                                  "componentId": 9999,
                                  "requestType": "CHANGE_REPLICAS",
                                  "requestedValue": "2",
                                  "requestedBy": "platform-operator"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Component not found")));
    }

    @Test
    void returnsNotFoundForUnknownRequest() throws Exception {
        mockMvc.perform(get("/api/cicd/requests/{id}", 9999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("CI/CD request not found: 9999")));
    }
}
