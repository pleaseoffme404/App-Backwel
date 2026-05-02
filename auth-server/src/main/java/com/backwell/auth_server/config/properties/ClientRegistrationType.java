package com.backwell.auth_server.config.properties;

import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

import java.time.Duration;
import java.util.List;

import static org.springframework.security.oauth2.core.AuthorizationGrantType.*;
import static org.springframework.security.oauth2.core.oidc.OidcScopes.*;

public enum ClientRegistrationType {
    GATEWAY {
        @Override
        public void configure(RegisteredClient.Builder builder, ClientRegistrationProperties.RegistrationProperties props) {
            builder
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantTypes(s ->
                            s.addAll(List.of(AUTHORIZATION_CODE, REFRESH_TOKEN, CLIENT_CREDENTIALS))
                    )
                    .postLogoutRedirectUri("http://localhost:3000/")
                    .redirectUri(props.getRedirectUri())
                    .scopes(s-> s.addAll(
                            List.of(OPENID, PROFILE, EMAIL, "offline_access")
                    ))
                    .tokenSettings(gatewayTokenSettings())
                    .clientSettings(gatewayClientSettings());
        }
    },
    SERVICE {
        @Override
        public void configure(RegisteredClient.Builder builder,  ClientRegistrationProperties.RegistrationProperties props) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(CLIENT_CREDENTIALS)
                    .scope("read")
                    .scope("write")
                    .scope("internal_api")
                    .tokenSettings(serviceTokenSettings());
        }
    };

    public abstract void configure (RegisteredClient.Builder builder, ClientRegistrationProperties.RegistrationProperties props);

    private static TokenSettings serviceTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofHours(1)) // Más tiempo para procesos largos
                .build();
    }

    private static TokenSettings gatewayTokenSettings() {
        return TokenSettings.builder()
                .accessTokenTimeToLive(Duration.ofMinutes(5))
                .refreshTokenTimeToLive(Duration.ofDays(30))
                .reuseRefreshTokens(false)
                .build();
    }

    private static ClientSettings gatewayClientSettings() {
        return ClientSettings.builder()
                .requireAuthorizationConsent(false)
                .requireProofKey(true)
                .build();
    }
}

