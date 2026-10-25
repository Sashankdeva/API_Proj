package com.squadup.config;

import com.squadup.repository.SquadRepository;
import com.squadup.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No profile is chosen here, exactly like {@code mvnw spring-boot:run}: the "dev" profile has to
 * apply by itself, so the local demo (console, demo players, open origins) keeps working.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:dev-defaults;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class DevDefaultsTest {

    @Autowired
    private Environment environment;

    @Autowired
    private UserRepository users;

    @Autowired
    private SquadRepository squads;

    @Test
    void devProfileIsTheDefault() {
        assertThat(environment.getActiveProfiles()).isEmpty();
        assertThat(environment.getDefaultProfiles()).contains("dev");
        assertThat(environment.getProperty("spring.h2.console.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("squadup.cors.allowed-origins")).isEqualTo("*");
        assertThat(users.count()).isEqualTo(20);
        assertThat(squads.count()).isEqualTo(18);
    }
}
