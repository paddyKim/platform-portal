package com.paddykim.platform.portal.command;

import static org.hamcrest.Matchers.is;
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
class ApplicationCommandControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void interpretsApplicationListCommands() throws Exception {
        mockMvc.perform(post("/api/application-commands/interpret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "command": "등록된 어플리케이션 목록 보여줘"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent", is("LIST_APPLICATIONS")))
                .andExpect(jsonPath("$.view", is("APPLICATION_LIST")));

        mockMvc.perform(post("/api/application-commands/interpret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "command": "app 리스트 조회"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent", is("LIST_APPLICATIONS")))
                .andExpect(jsonPath("$.view", is("APPLICATION_LIST")));
    }

    @Test
    void interpretsApplicationCreateCommands() throws Exception {
        mockMvc.perform(post("/api/application-commands/interpret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "command": "신규 어플리케이션 등록할게"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent", is("OPEN_APPLICATION_CREATE_FORM")))
                .andExpect(jsonPath("$.view", is("APPLICATION_CREATE")));

        mockMvc.perform(post("/api/application-commands/interpret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "command": "새로운 app 등록할게"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent", is("OPEN_APPLICATION_CREATE_FORM")))
                .andExpect(jsonPath("$.view", is("APPLICATION_CREATE")));
    }

    @Test
    void returnsUnknownForUnsupportedCommand() throws Exception {
        mockMvc.perform(post("/api/application-commands/interpret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "command": "오늘 상태 요약해줘"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intent", is("UNKNOWN")))
                .andExpect(jsonPath("$.view", is("NONE")));
    }
}
