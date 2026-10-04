package com.squadup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point of the SquadUp player matching platform.
 *
 * {@code @SpringBootApplication} = {@code @Configuration} + {@code @EnableAutoConfiguration}
 * + {@code @ComponentScan}. Component scanning starts from this package, so every
 * {@code @Component}, {@code @Service}, {@code @Repository} and {@code @Controller}
 * under {@code com.squadup.*} is discovered and registered in the IoC container.
 */
@SpringBootApplication
public class SquadUpApplication {

    public static void main(String[] args) {
        SpringApplication.run(SquadUpApplication.class, args);
    }
}
