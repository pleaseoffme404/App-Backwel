package com.backwell.auth_server.dto.response;

import com.backwell.auth_server.jpa.entity.Role;

import java.util.List;
import java.util.UUID;

public record RoleDTO(
        UUID roleId,
        String name,
        List<PermissionDTO> permissions
) {
    public static RoleDTO fromEntity(Role r) {
        return new RoleDTO(
                r.getId(),
                r.getName(),
                r.getPermissions()
                        .stream()
                        .map(PermissionDTO::fromEntity)
                        .toList()
        );
    }
}
