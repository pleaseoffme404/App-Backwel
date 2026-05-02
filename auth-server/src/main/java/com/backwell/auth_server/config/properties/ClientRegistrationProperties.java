package com.backwell.auth_server.config.properties;

import com.backwell.auth_server.service.UUIDGeneratorService;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.client-registration")
@Data
public class ClientRegistrationProperties {
    private Map<ClientRegistrationType, RegistrationProperties> clients;

    @Getter
    @AllArgsConstructor
    public static class RegistrationProperties {
        private String clientId;
        private String secret;
        private String redirectUri;
        private String accessTokenTtl;
        private String refreshTokenTtl;
    }

    public List<RegisteredClient> buildAll(PasswordEncoder passwordEncoder, UUIDGeneratorService uuidService) {
        return clients.entrySet().stream()
                .map(entry -> {
                    ClientRegistrationType type = entry.getKey();
                    RegistrationProperties props = entry.getValue();

                    String id = uuidService.generate().toString();

                    var builder = RegisteredClient
                            .withId(id)
                            .clientId(props.clientId)
                            .clientSecret(passwordEncoder.encode(props.secret));
                    type.configure(builder, props);
                    return builder.build();
                })
                .toList();
    }
}
