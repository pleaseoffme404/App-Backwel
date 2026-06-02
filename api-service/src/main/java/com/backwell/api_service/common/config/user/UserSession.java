package com.backwell.api_service.common.config.user;

import com.backwell.enums.AuthProvider;
import com.backwell.enums.RoleName;
import static com.backwell.enums.JwtClaim.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public record UserSession (
        UUID uuid,
        String email,
        RoleName highestRole,
        Set<RoleName> roles,
        AuthProvider authProvider
) {

    public static UserSession fromJwt(Jwt jwt) {
        String userIdStr = jwt.getClaimAsString(USER_ID.key());
        UUID uuid = userIdStr != null ? UUID.fromString(userIdStr) : null;

        String email = jwt.getClaimAsString(EMAIL.key());

        List<String> rawRoles = Optional.ofNullable(jwt.getClaimAsStringList(ROLES.key()))
                .orElse(List.of());

        log.info("User session processed with raw roles: [{}]", rawRoles);

        Set<RoleName> roles = !rawRoles.isEmpty()
                ? rawRoles.stream().map(RoleName::fromString).collect(Collectors.toSet())
                : Set.of(RoleName.USER);

        RoleName highestRole = RoleName.getHighestRole(roles);

        String providerStr = jwt.getClaimAsString(AUTH_PROVIDER.key());
        AuthProvider authProvider = parseAuthProvider(providerStr);

        return new UserSession(uuid, email, highestRole, roles, authProvider);
    }

    private static AuthProvider parseAuthProvider(String providerStr) {
        if (providerStr == null) {
            return AuthProvider.LOCAL;
        }
        try {
            return AuthProvider.valueOf(providerStr.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown AuthProvider received: [{}]. Falling back to LOCAL.", providerStr);
            return AuthProvider.LOCAL;
        }
    }
}