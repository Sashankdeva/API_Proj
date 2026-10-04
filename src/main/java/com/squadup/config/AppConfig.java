package com.squadup.config;

import com.squadup.service.ChatModerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.List;

/**
 * Java-based configuration of the IoC container.
 *
 * Each {@code @Bean} method registers the object it returns as a (singleton) bean. This is
 * the way to put classes into the container that we do not annotate ourselves: library
 * classes such as {@link BCryptPasswordEncoder}, or plain POJOs such as {@link ChatModerator}.
 */
@Configuration
@EnableScheduling
public class AppConfig {

    /** Hashes passwords. Reads the "$2a$/$2b$" hashes produced by bcryptjs in the old Node app too. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /** A single source of "now", so time-dependent code (token expiry) can be tested with a fixed clock. */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecureRandom secureRandom() {
        return new SecureRandom();
    }

    /**
     * Setter injection: the moderator is created with its no-arg constructor and then
     * configured through setters, using values from application.properties.
     */
    @Bean
    public ChatModerator chatModerator(
            @Value("${squadup.chat.banned-words:}") List<String> bannedWords,
            @Value("${squadup.chat.max-length:2000}") int maxLength) {
        ChatModerator moderator = new ChatModerator();
        moderator.setBannedWords(bannedWords);
        moderator.setMaxLength(maxLength);
        return moderator;
    }
}
