package com.disthropic.api_gateway.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConfigurationProperties(prefix="oauth2.client")
@Data
public class OAuth2ClientProperties {
    @NotBlank(message = "Client ID is required")
    private String clientId;

    @NotBlank(message = "Client Secret is required")
    private String clientSecret;

    private String registrationId = "gateway-client";
    private String redirectUri = "{baseUrl}/login/oauth2/code/{registrationId}";
    private String[] scopes = {"openid", "profile", "email", "offline_access"};

    private boolean pkceEnabled = true;
    private boolean requireProofKey = true;

    private InternalUrls internal = new InternalUrls();
    private ExternalUrls external = new ExternalUrls();

    @Data
    public static class InternalUrls {
        @NotNull
        private String authServerUrl = "http://auth-server:9000";
        private String authorizationUri = authServerUrl + "/oauth2/authorize";
        private String tokenUri = authServerUrl + "/oauth2/token";
        private String jwkSetUri = authServerUrl + "/oauth2/jwks";
    }

    @Data
    public static class ExternalUrls {
        @NotNull
        private String authServerUrl = "http://localhost:9000";
        private String authorizationUri = authServerUrl + "/oauth2/authorize";
    }

}
