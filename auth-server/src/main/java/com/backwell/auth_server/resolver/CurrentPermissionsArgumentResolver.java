package com.backwell.auth_server.resolver;

import com.backwell.auth_server.security.user.UserDTO;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;


/**
 * Extracts the current user-'s roleId and permissions set into a {@link CurrentPermissionsContainer} object
 * @implNote For general purposes, use {@link UserDTOArgumentResolver} implementation through {@link CurrentUser}
 * annotation. Reserve this implementation for fully role-permissions-based operations.
 * For most cases, this implementation is unnecessary.
 * @see CurrentPermissions*/
@Component
public class CurrentPermissionsArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentPermissions.class)
                && parameter.getParameterType().isAssignableFrom(CurrentPermissionsContainer.class);
    }

    @Override
    @NonNull
    public Object resolveArgument(
            MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if  (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException(
                    "No valid authentication was found in the security context to resolve the parameter: '%s'"
                            .formatted(parameter.getParameterName())
            );
        }

        if (authentication.getPrincipal() instanceof UserDTO dto) {
            return new CurrentPermissionsContainer(
                    dto.roleId(),
                    dto.permissionNames()
            );
        }

        throw new IllegalStateException(
                "Authentication Principal does not match IdentityContainer class. (Type found: [%s])"
                        .formatted(parameter.getParameterName())
        );
    }
}
