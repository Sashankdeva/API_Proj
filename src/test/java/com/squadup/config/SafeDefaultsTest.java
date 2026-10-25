package com.squadup.config;

import com.squadup.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Starts the application with the real application.properties and a profile other than "dev".
 * Everything that is only meant for a local run has to be off.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:safe-defaults;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("safe")
class SafeDefaultsTest {

    @Autowired
    private Environment environment;

    @Autowired
    private UserRepository users;

    @Autowired
    private MockMvc mvc;

    @Test
    void consoleDemoDataAndCrossOriginAccessAreOffOutsideDev() throws Exception {
        assertThat(environment.getProperty("spring.h2.console.enabled", Boolean.class)).isFalse();
        assertThat(users.count()).isZero();

        mvc.perform(get("/h2-console/")).andExpect(status().isUnauthorized());
        mvc.perform(options("/api/v1/squads")
                        .header("Origin", "https://other.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
