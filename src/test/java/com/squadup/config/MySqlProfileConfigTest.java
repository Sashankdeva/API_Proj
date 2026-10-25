package com.squadup.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MySqlProfileConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MySqlProfileConfig.class)
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("mysql"));

    @Test
    void startStopsWithAClearMessageWhenThePasswordIsMissing() {
        runner.run(context -> assertThat(context).hasFailed().getFailure()
                .hasStackTraceContaining("needs the environment variable DB_PASSWORD"));
    }

    @Test
    void startContinuesWhenThePasswordIsSet() {
        runner.withPropertyValues("DB_PASSWORD=secret").run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void otherProfilesAreNotAffected() {
        new ApplicationContextRunner().withUserConfiguration(MySqlProfileConfig.class)
                .run(context -> assertThat(context).hasNotFailed());
    }
}
