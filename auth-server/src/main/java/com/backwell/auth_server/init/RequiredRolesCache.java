package com.backwell.auth_server.init;

import com.backwell.auth_server.config.properties.DefaultOwnerProperties;
import com.backwell.auth_server.config.properties.DefaultUserRoleProperties;
import com.backwell.auth_server.jpa.entity.Role;
import com.backwell.auth_server.jpa.repo.RoleRepository;
import com.backwell.auth_server.util.UUIDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.backwell.auth_server.init.RequiredRole.*;

@Component
@Slf4j
@Order(3)
public class RequiredRolesCache implements ApplicationInitializer {

    private final UUIDService uuidService;
    private final RoleRepository roleRepository;
    private final PermissionsCache permissionsCache;
    private final DefaultOwnerProperties ownerProperties;
    private final DefaultUserRoleProperties userProperties;

    private final Map<RequiredRole, UUID> discreteCache = new ConcurrentHashMap<>();

    public RequiredRolesCache(
            UUIDService uuidService,
            RoleRepository roleRepository,
            PermissionsCache permissionsCache,
            DefaultOwnerProperties ownerProperties,
            DefaultUserRoleProperties userProperties
    ) {
        this.uuidService = uuidService;
        this.roleRepository = roleRepository;
        this.permissionsCache = permissionsCache;
        this.ownerProperties = ownerProperties;
        this.userProperties = userProperties;
    }

    @Override
    @Transactional
    public void initialize() {
        log.info("Starting RequiredRolesCache system synchronization...");

        Role ownerEntity = computeOwnerRole();
        Role userEntity = computeDefaultRole();

        discreteCache.put(OWNER, ownerEntity.getId());
        discreteCache.put(USER, userEntity.getId());

        log.info("RequiredRolesCache successfully initialized with verified discrete UUIDs.");
    }

    /**
     * Devuelve un Proxy basado en el UUID de la caché discreta.
     */
    public Role getReference(RequiredRole requiredRole) {
        UUID roleId = discreteCache.get(requiredRole);
        if (roleId == null) {
            throw new IllegalStateException(
                    "RequiredRolesCache has not been initialized yet or role '%s' is missing."
                            .formatted(requiredRole)
            );
        }
        return roleRepository.getReferenceById(roleId);
    }

    public Role getOwnerReference() {
        return getReference(OWNER);
    }

    public Role getUserReference() {
        return getReference(USER);
    }

    private Role computeOwnerRole() {
        Optional<Role> existingOwner = roleRepository.findByName(ownerProperties.roleName());
        if (existingOwner.isEmpty()) {
            log.info("Role '{}' not found in DB. Proceeding with clean creation", ownerProperties.roleName());
            Role newOwnerRole = Role.builder()
                    .id(uuidService.next())
                    .name(ownerProperties.roleName())
                    .permissions(permissionsCache.getAll())
                    .build();
            Role savedRole = roleRepository.save(newOwnerRole);
            log.info("Role '{}' initialized successfully with all active system permissions.", ownerProperties.roleName());
            return savedRole;
        }

        Role ownerRole = existingOwner.get();
        if (!permissionsCache.hasAllActivePermissions(ownerRole.getPermissions())) {
            log.warn("Synchronization gap detected for role '{}'. Injecting missing or updated permissions...", ownerProperties.roleName());

            ownerRole.setPermissions(permissionsCache.getAll());
            Role updatedRole = roleRepository.save(ownerRole);

            log.info("Role '{}' updated successfully with latest system permissions.", ownerProperties.roleName());
            return updatedRole;
        } else {
            log.info("Role '{}' is already fully synchronized with all active permissions.", ownerProperties.roleName());
            return ownerRole;
        }
    }

    private Role computeDefaultRole() {
        String targetRoleName = userProperties.name();
        Optional<Role> existingRole = roleRepository.findByName(targetRoleName);

        if (existingRole.isPresent()) {
            log.info("Default user role '{}' verified in DB.", targetRoleName);
            return existingRole.get();
        } else {
            log.warn("Default user role '{}' not found. Proceeding to create one with DEFAULT values", targetRoleName);
            Role newUserRole = Role.builder()
                    .id(uuidService.next())
                    .name(targetRoleName)
                    .permissions(Set.of())
                    .build();

            Role savedRole = roleRepository.save(newUserRole);
            log.info("Default user role '{}' created successfully.", targetRoleName);
            return savedRole;
        }
    }

    /**
     * @implNote Weak implementation, will need an optimization when required roles grow bigger*/
    public boolean isRequiredRole(UUID roleId) {
        return discreteCache.containsValue(roleId);
    }


    /**
     * Helper method, validates if the UUID matches the cached OWNER role ID*/
    public boolean isOwner(UUID roleId) {
        return discreteCache.get(OWNER).equals(roleId);
    }

    public boolean isOwner(Role role) {
        return isOwner(role.getId());
    }
}