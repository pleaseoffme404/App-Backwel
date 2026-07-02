package com.backwell.auth_server.config;

import com.backwell.auth_server.security.user.IdentityContainer;
import com.backwell.auth_server.security.user.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.stereotype.Component;
import static com.backwell.enums.JwtClaim.*;

@Component
@Slf4j
public class TokenClaimsEnhancer implements OAuth2TokenCustomizer<JwtEncodingContext> {

    @Override
    public void customize(JwtEncodingContext context) {

        if (OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType()) ||
                OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())) {

            Authentication authentication = context.getPrincipal();

            if (authentication != null) {
                var claims = context.getClaims();
                context.getClaims().claim(USER_ID.key(), authentication.getName());

                // 1. Intentamos extraer el objeto de identidad del usuario
                IdentityContainer container = getIdentityContainer(authentication);

                // 2. Si logramos recuperar tu contenedor, inyectamos las claims con tu UserDTO
                if (container != null) {
                    log.info("¡Identity Container detectado con éxito!: {}", container);
                    UserDTO user = container.getUserDTO();

                    claims.claim(USER_UUID.key(), user.uuid().toString());
                    claims.claim(EMAIL.key(), user.email());


                    log.info("[TOKEN CLAIMS] Signing token with Role: {} and permissions HEX: {}.", user.role(), user.permissionsHex());
                    claims.claim(ROLE.key(), user.role());
                    claims.claim(PERMISSIONS.key(), user.permissionsHex());

                    claims.claim(AUTH_PROVIDER.key(), user.authProvider());
                } else {
                    log.warn("No se pudo mapear a IdentityContainer. El principal interno es de tipo: {}",
                            authentication.getPrincipal() != null ? authentication.getPrincipal().getClass().getName() : "null");
                }
            }
        }
    }

    private IdentityContainer getIdentityContainer(Authentication authentication) {
        IdentityContainer container = null;

        // Escenario A: Tu clase es directamente el principal de la autenticación
        if (authentication.getPrincipal() instanceof IdentityContainer idContainer) {
            container = idContainer;
        }
        // Escenario B: Por si el principal es otra capa de autenticación, pero el objeto Authentication mismo implementa la interfaz
        else if (authentication instanceof IdentityContainer idContainer) {
            container = idContainer;
        }
        return container;
    }
}
