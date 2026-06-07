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

        if (
                OAuth2TokenType.ACCESS_TOKEN.equals(context.getTokenType()) ||
                OidcParameterNames.ID_TOKEN.equals(context.getTokenType().getValue())
        ) {

            Authentication principal = context.getPrincipal();

            if (principal != null) {
                var claims = context.getClaims();
                context.getClaims().claim(USER_ID.key(), principal.getName());

                if(principal instanceof IdentityContainer container) {
                    UserDTO user = container.getUserDTO();



                    claims.claim(USER_UUID.key(), user.uuid().toString());
                    claims.claim(EMAIL.key(), user.email());

                    log.debug("Token signed with roles: {}",  user.roles());
                    claims.claim(ROLES.key(), user.roles());

                    claims.claim(AUTH_PROVIDER.key(),  user.authProvider());
                }
            }
        }

    }
}
