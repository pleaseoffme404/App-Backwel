package com.backwell.auth_server.security.user;

import com.backwell.auth_server.jpa.entity.Permission;
import com.backwell.auth_server.jpa.entity.Role;
import com.backwell.auth_server.jpa.entity.User;
import com.backwell.enums.AuthProvider;
import com.backwell.enums.PermissionName;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public record UserDTO(
        UUID uuid,
        String email,
        String password,
        AuthProvider authProvider,
        UUID roleId,
        String role,
        Set<PermissionName> permissionNames,
        String permissionsHex,
        boolean expired,
        boolean locked,
        boolean credentialsExpired,
        boolean disabled
) {
    public static UserDTO fromEntity(User e) {
        Role r =  e.getRole();

        Set<PermissionName> permissionsSet = e.getRole().getPermissions().stream()
                .map(Permission::getPermissionName)
                .collect(Collectors.toSet());

        String permissionsHex = PermissionName.toHexBitMask(e.getPermissionName());

        return new UserDTO(
                e.getId(),
                e.getEmail(),
                e.getPassword(),
                e.getAuthProvider(),
                r.getId(),
                r.getName(),
                permissionsSet,
                permissionsHex,
                e.isExpired(),
                e.isLocked(),
                e.isCredentialsExpired(),
                e.isDisabled()
        );
    }
}
