package com.squadup.config;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/**
 * The {@code mysql} profile has no default password (see application-mysql.properties). Without
 * this check an unset DB_PASSWORD would be used as the literal text "${DB_PASSWORD}" and the
 * start would end in a confusing connection error, so it stops early with a clear message.
 */
@Configuration
@Profile("mysql")
public class MySqlProfileConfig {

    @Bean
    static BeanFactoryPostProcessor requireDatabasePassword(Environment environment) {
        return beanFactory -> {
            if (!environment.containsProperty("DB_PASSWORD")) {
                throw new IllegalStateException(
                        "The mysql profile needs the environment variable DB_PASSWORD (and optionally DB_USERNAME).");
            }
        };
    }
}
