package com.backwell.api_service.common.config;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.enums.RoleName;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Utility component exposed to Spring Security's expression language (SpEL)
 * for evaluating custom authorization rules.
 *
 * <p>
 * This bean is registered with the name {@code "auth"}, allowing it to be
 * referenced from {@link org.springframework.security.access.prepost.PreAuthorize}
 * expressions such as:
 * </p>
 *
 * <pre>{@code
 * @PreAuthorize("@auth.hasAuthority(T(com.backwell.enums.RoleName).MANAGER)")
 * }</pre>
 *
 * <p>
 * Authorization is based on the highest role assigned to the authenticated
 * user. A user is considered authorized if their highest role satisfies the
 * hierarchical range defined by {@link RoleName#hasRange(RoleName)}.
 * </p>
 */
@Component("auth")
public class SecurityService {

    /**
     * Determines whether the currently authenticated user has sufficient
     * privileges for the requested role.
     *
     * <p>
     * The user's highest assigned role is resolved and compared against the
     * required role using the role hierarchy implemented by
     * {@link RoleName#hasRange(RoleName)}.
     * </p>
     *
     * @param requiredRole the minimum role required to perform the operation.
     * @return {@code true} if the current user has the required authority or
     *         a higher one in the hierarchy; {@code false} otherwise.
     */
    public boolean hasAuthority(RoleName requiredRole) {
        UserSession session = getUserSession();

        if (session == null || session.roles() == null || session.roles().isEmpty()) {
            return false;
        }

        RoleName highestRole = RoleName.getHighestRole(session.roles());

        if (highestRole == null) {
            return false;
        }

        return highestRole.hasRange(requiredRole);
    }

    /**
     * Extracts the current authenticated user's session information from the
     * Spring Security context.
     *
     * <p>
     * If the current authentication principal is a JWT, it is converted into
     * a {@link UserSession}; otherwise, {@code null} is returned.
     * </p>
     *
     * @return the current {@link UserSession}, or {@code null} if no valid
     *         authenticated JWT is available.
     */
    private UserSession getUserSession() {
        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return UserSession.fromJwt(jwt);
        }

        return null;
    }
}