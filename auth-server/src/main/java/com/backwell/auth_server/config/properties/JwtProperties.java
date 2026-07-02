package com.backwell.auth_server.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties (
        String privateKey,
        String publicKey,
        String keyId,
        String issuer
) {

}