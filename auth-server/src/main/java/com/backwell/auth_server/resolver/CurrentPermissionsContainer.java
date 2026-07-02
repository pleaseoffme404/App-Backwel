package com.backwell.auth_server.resolver;

import com.backwell.enums.PermissionName;

import java.util.Set;
import java.util.UUID;

public record CurrentPermissionsContainer(
        UUID roleId,
        Set<PermissionName> permissions
) {
}
