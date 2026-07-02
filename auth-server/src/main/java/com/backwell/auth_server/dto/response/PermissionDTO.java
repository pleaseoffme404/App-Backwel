package com.backwell.auth_server.dto.response;

import com.backwell.auth_server.jpa.entity.Permission;

import java.util.UUID;

public record PermissionDTO (
        UUID permissionId,
        String name
) {
    public static PermissionDTO fromEntity(Permission p) {
        return new PermissionDTO(
                p.getId(),
                p.getPermissionNameString()
        );
    }

}
