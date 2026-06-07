package com.backwell.api_service.common.config.user;

import com.backwell.enums.AuthProvider;
import com.backwell.enums.RoleName;
import static com.backwell.enums.JwtClaim.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;
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
        log.info("[DEBUG JWT] Full claims map payload: {}", jwt.getClaims());

        String userIdStr = jwt.getClaimAsString(USER_ID.key());
        UUID uuid = userIdStr != null ? UUID.fromString(userIdStr) : null;

        String email = jwt.getClaimAsString(EMAIL.key());

        List<String> rawRoles = new ArrayList<>();

        if (jwt.hasClaim(ROLES.key())){
            List<String> claimsList = jwt.getClaimAsStringList(ROLES.key());
            if (claimsList != null) {
                rawRoles = claimsList;
            }
        } else {
            log.warn("[DEBUG JWT] The configured ROLES claim key [{}] WAS NOT FOUND in this token.", ROLES.key());
        }

        log.info("[DEBUG JWT] Roles processed for this session: {}", rawRoles);

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