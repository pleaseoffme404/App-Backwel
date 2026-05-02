package com.backwell.api_service.common.config.user;

import com.backwell.enums.AuthProvider;
import com.backwell.enums.RoleName;
import static com.backwell.enums.JwtClaim.*;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserSession (
        UUID uuid,
        String email,
        Set<RoleName> roles,
        AuthProvider authProvider
) {

    public static UserSession fromJwt(Jwt jwt) {

        UUID uuid = UUID.fromString(jwt.getClaim(USER_ID.key()));
        String email = jwt.getClaim(EMAIL.key());
        Set<RoleName> roles = jwt.getClaimAsStringList(ROLES.key()).stream()
                .map(RoleName::fromString)
                .collect(Collectors.toSet());
        AuthProvider authProvider = AuthProvider.valueOf(jwt.getClaim(AUTH_PROVIDER.key()));
        return new UserSession(uuid, email, roles, authProvider);
    }
}
