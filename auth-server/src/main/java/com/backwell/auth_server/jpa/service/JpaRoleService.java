package com.backwell.auth_server.jpa.service;

import com.backwell.auth_server.dto.request.CreateRoleRequest;
import com.backwell.auth_server.dto.request.UpdateRoleRequest;
import com.backwell.auth_server.dto.response.MessageResponse;
import com.backwell.auth_server.dto.response.RoleDTO;
import com.backwell.auth_server.exception.role.RoleNameConflictException;
import com.backwell.auth_server.init.PermissionsCache;
import com.backwell.auth_server.init.RequiredRolesCache;
import com.backwell.auth_server.jpa.entity.Permission;
import com.backwell.auth_server.jpa.entity.Role;
import com.backwell.auth_server.jpa.entity.User;
import com.backwell.auth_server.jpa.repo.RoleRepository;
import com.backwell.auth_server.jpa.repo.UserRepository;
import com.backwell.auth_server.security.roles.RoleAuthorizationPolicyEngine;
import com.backwell.auth_server.util.UUIDService;
import com.backwell.enums.PermissionName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JpaRoleService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UUIDService uuidService;
    private final PermissionsCache permissionsCache;
    private final RequiredRolesCache rolesCache;
    private final RoleAuthorizationPolicyEngine authorizationPolicyEngine;

    @Transactional
     public RoleDTO createRole(
            UUID actorRoleId,
            Set<PermissionName> actorPermissions,
            CreateRoleRequest request
    ) {
        if (roleRepository.findByName(request.name()).isPresent()) {
            throw new RoleNameConflictException("Role Name: '%s already exists'".formatted(request.name()));
        }

        Set<Permission> permissions = request.permissionsSet().stream()
                .map(permissionsCache::get)
                .collect(Collectors.toSet());

        Set<PermissionName> permissionNames = permissions.stream()
                .map(Permission::getPermissionName)
                .collect(Collectors.toSet());

        authorizationPolicyEngine.validateAssignmentPolicy(actorRoleId, actorPermissions, permissionNames);

        Role newRole = Role.builder()
                .id(uuidService.next())
                .name(request.name())
                .permissions(permissions)
                .build();

        Role saved = roleRepository.save(newRole);
        return RoleDTO.fromEntity(saved);
    }

    @Transactional
    public RoleDTO updateRole(
            UUID actorRoleId,
            Set<PermissionName> actorPermissions,
            UUID targetRoleId,
            UpdateRoleRequest req
    ) {

        Role targetRole = roleRepository.getOrThrow(targetRoleId);

        // OWNER or USER roles can not be modified
        if (rolesCache.isRequiredRole(targetRoleId)) {
            throw new IllegalArgumentException("Targeted Role is flagged as a Required Role and can not be updated.");
        }


        req.getNewNameOpt().ifPresent(newName -> {
            if (!targetRole.getName().equals(newName) && roleRepository.violatesUniqueName(newName)) {
                throw new RoleNameConflictException(
                        "Could not update Role Name to '%s'. This name is used by another role.".formatted(newName)
                );
            }
            targetRole.setName(newName);
        });

        req.getNewPermissionsSetOpt().ifPresent(permissions -> {
            Set<PermissionName> newPermissions = permissions.stream()
                    .map(permissionsCache::getPermissionName)
                    .collect(Collectors.toSet());

            Set<PermissionName> previousPermissions = targetRole.getPermissionNamesSet();

            checkUpdateOperation(
                    actorRoleId,
                    actorPermissions,
                    previousPermissions,
                    newPermissions
            );

            Set<Permission> newPermissionEntities = newPermissions.stream()
                    .map(permissionsCache::get)
                    .collect(Collectors.toSet());

            targetRole.setPermissions(newPermissionEntities);
        });

        Role updated = roleRepository.save(targetRole);
        return RoleDTO.fromEntity(updated);
    }

    @Transactional
    public MessageResponse updateUserRole(
            UUID actorRoleId,
            Set<PermissionName> actorPermissions,
            UUID targetUserId,
            UUID newTargetRoleId
    ) {
        User targetUser = userRepository.getOrThrow(targetUserId);
        Role targetRole = targetUser.getRole();
        if (rolesCache.isOwner(targetRole)) {
            throw new AccessDeniedException("Requested User has OWNER Access Level and can not be modified.");
        }

        Role newTargetRole = roleRepository.getOrThrow(newTargetRoleId);

        checkUpdateOperation(
                actorRoleId,
                actorPermissions,
                targetRole.getPermissionNamesSet(),
                newTargetRole.getPermissionNamesSet()
        );

        // if no exception was thrown, then the movement can be committed
        targetUser.setRole(newTargetRole);
        userRepository.save(targetUser);

        return new MessageResponse("Role updated successfully.");
    }



    /**
     * If actor User has permissions to revoke the target,
     * sets the target user role with the de facto USER from {@link RequiredRolesCache}*/
    @Transactional
    public MessageResponse revokeUser(
            UUID actorRoleId,
            Set<PermissionName> actorPermissions,
            UUID targetUserId
    ){
        User targetUser =  userRepository.getOrThrow(targetUserId);
        Role targetRole = targetUser.getRole();

        if (rolesCache.isOwner(targetRole)) {
            throw new AccessDeniedException("Requested User has OWNER Access Level and can not be revoked.");
        }

        authorizationPolicyEngine.validateRevocationPolicy(actorRoleId, actorPermissions, targetRole.getPermissionNamesSet(), Set.of());

        targetUser.setRole(rolesCache.getUserReference());
        userRepository.save(targetUser);
        return new MessageResponse("User revoked successfully.");
    }


    private void checkUpdateOperation(
            UUID actorRoleId,
            Set<PermissionName> actorPermissions,
            Set<PermissionName> previousPermissions,
            Set<PermissionName> newPermissions
    ) {
        Set<PermissionName> toGrant = newPermissions.stream()
                .filter(p -> !previousPermissions.contains(p))
                .collect(Collectors.toSet());

        Set<PermissionName> toRevoke = previousPermissions.stream()
                .filter(p -> !newPermissions.contains(p))
                .collect(Collectors.toSet());

        authorizationPolicyEngine.validateRevocationPolicy(actorRoleId, actorPermissions, toGrant, toRevoke);
        authorizationPolicyEngine.validateAssignmentPolicy(actorRoleId, actorPermissions, toGrant);
    }
}
