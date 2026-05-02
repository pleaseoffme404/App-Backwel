package com.backwell.api_service.common.config;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.enums.RoleName;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component("auth")
public class SecurityService {


    public boolean isAdmin() {
        return hasRole(RoleName.ADMIN);
    }

    public boolean isManager() {
        return hasRole(RoleName.MANAGER);
    }

    public boolean isOwner() {
        return hasRole(RoleName.OWNER);
    }

    public boolean hasRole(RoleName role) {
        UserSession session = getUserSession();
        if (session == null) return false;

        return session.roles().contains(role);
    }

    private UserSession getUserSession() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return UserSession.fromJwt(jwt);
        }
        return null;
    }
}
