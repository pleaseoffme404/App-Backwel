package com.backwell.auth_server.config;

import com.backwell.auth_server.security.user.IdentityContainer;
import com.backwell.auth_server.security.user.UserDTO;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.stereotype.Component;
import static com.backwell.enums.JwtClaim.*;

import java.util.Map;

@Component
public class TokenClaimsEnhancer {
    public void customizeClaims(Map<String, Object> claims, JwtEncodingContext context) {
        claims.put(USER_ID.key(), context.getPrincipal().getName());

        // Extraer info de la identidad personalizada
        if (context.getPrincipal().getPrincipal() instanceof IdentityContainer container) {
            UserDTO user = container.getUserDTO();

            claims.put(USER_UUID.key(), user.uuid().toString());
            claims.put(EMAIL.key(), user.email());
            claims.put(ROLES.key(), user.roles());
            claims.put(AUTH_PROVIDER.key(),  user.authProvider());
        }
    }
}
