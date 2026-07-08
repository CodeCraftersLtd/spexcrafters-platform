package com.spexcrafters.identity.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.spexcrafters.identity.infrastructure.config.AppSecurityProperties;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Symmetric HS256 JWT setup for Sprint 1 (single deployable signs and verifies its own
 * tokens). ADR-006 upgrades this to Authorization Code + PKCE with Spring Authorization
 * Server in Sprint 2.
 */
@Configuration
public class JwtConfig {

    @Bean
    SecretKey jwtSecretKey(AppSecurityProperties properties) {
        String secret = properties.jwtSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "spexcrafters.security.jwt-secret must be set and at least 32 bytes long for HS256");
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey jwtSecretKey) {
        return NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
