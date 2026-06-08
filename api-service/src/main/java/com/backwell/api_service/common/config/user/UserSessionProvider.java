package com.backwell.api_service.common.config.user;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserSessionProvider {

    public UserSession getCurrentUserSession() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return UserSession.fromJwt(jwt);
        }
        return null;
    }

    public Optional<UserSession> getCurrentUserSessionOptional() {
        return Optional.ofNullable(getCurrentUserSession());
    }
}
