package com.backwell.auth_server.security.checker;

import com.backwell.auth_server.security.user.IdentityContainer;
import com.backwell.enums.PermissionName;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component("securityChecker")
public class SecurityChecker {

    public boolean hasPermission(String permission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        PermissionName requestedPermission = getPermissionNameOrThrow(permission);

        var principal = authentication.getPrincipal();
        if  (Objects.isNull(principal)) {
            return false;
        }

        if (principal instanceof IdentityContainer container) {
            return container.getUserDTO().permissionNames().contains(requestedPermission);
        }
        return false;
    }

    public boolean hasPermissions(Set<String> permissions) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null) {
            return false;
        }

        Set<PermissionName> requestedPermissions = permissions.stream()
                .map(this::getPermissionNameOrThrow)
                .collect(Collectors.toSet());

        if (authentication.getPrincipal() instanceof IdentityContainer container) {
            return container.getUserDTO().permissionNames().containsAll(requestedPermissions);
        }
        return false;
    }

    private PermissionName getPermissionNameOrThrow(String permissionString) {
        return PermissionName.fromString(permissionString)
                .orElseThrow(() -> new IllegalStateException("Requested permission '%s does not exist'".formatted(permissionString)));
    }


}
