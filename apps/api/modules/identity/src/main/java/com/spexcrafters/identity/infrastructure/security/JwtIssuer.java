package com.spexcrafters.identity.infrastructure.security;

import com.spexcrafters.identity.infrastructure.config.AuthProperties;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Issues the short-lived HS256 access tokens described by the OpenAPI contract. */
@Component
public class JwtIssuer {

    public static final String ISSUER = "spexcrafters";

    private final JwtEncoder jwtEncoder;
    private final AuthProperties authProperties;

    public JwtIssuer(JwtEncoder jwtEncoder, AuthProperties authProperties) {
        this.jwtEncoder = jwtEncoder;
        this.authProperties = authProperties;
    }

    public String issueAccessToken(UUID userId, String email, Instant now) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plus(authProperties.accessTokenTtl()))
                .claim("email", email)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
