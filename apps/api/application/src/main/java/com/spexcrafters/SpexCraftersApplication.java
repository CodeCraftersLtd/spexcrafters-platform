package com.spexcrafters;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Boots the modular monolith. Rooted at {@code com.spexcrafters} so component scanning,
 * entity scanning and repository scanning cover every module
 * ({@code com.spexcrafters.<context>.*}) without per-module configuration.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class SpexCraftersApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpexCraftersApplication.class, args);
    }
}
