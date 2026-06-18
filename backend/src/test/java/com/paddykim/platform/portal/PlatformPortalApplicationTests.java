package com.paddykim.platform.portal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PlatformPortalApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void healthResponseIdentifiesPortalApi() {
        HealthController controller = new HealthController();

        Map<String, Object> response = controller.health();

        assertThat(response)
                .containsEntry("status", "ok")
                .containsEntry("service", "platform-portal-api");
        assertThat(response.get("timestamp")).isInstanceOf(String.class);
    }
}
