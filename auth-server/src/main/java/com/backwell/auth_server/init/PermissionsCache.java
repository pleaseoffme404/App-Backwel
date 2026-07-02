package com.backwell.auth_server.init;

import com.backwell.auth_server.jpa.entity.Permission;
import com.backwell.auth_server.jpa.repo.PermissionRepository;
import com.backwell.auth_server.util.UUIDService;
import com.backwell.enums.PermissionName;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
@Slf4j
@Order(2)
@Validated
public class PermissionsCache implements ApplicationInitializer {
    private final PermissionRepository permissionRepository;
    private final UUIDService uuidService;

    private final Map<PermissionName, Permission> permissionsMap = new ConcurrentHashMap<>();
    private final Map<UUID, Permission> uuidPermissionMap = new ConcurrentHashMap<>();

    public PermissionsCache(PermissionRepository permissionRepository, UUIDService uuidService) {
        this.permissionRepository = permissionRepository;
        this.uuidService = uuidService;
    }

    @Override
    @Transactional
    public void initialize() {
        init();
    }

    private void init() {
        log.info("Starting PermissionsCache Initialization and sync...");

        try {
            List<String> dbPermissionNames = permissionRepository.getDbPermissionNames();

            Set<String> obsoleteNames = new HashSet<>();
            Set<PermissionName> validNamesInDb = new HashSet<>();

            for (String dbName : dbPermissionNames) {
                try {
                    PermissionName enumName = PermissionName.valueOf(dbName);
                    validNamesInDb.add(enumName);
                } catch (IllegalArgumentException e) {
                    obsoleteNames.add(dbName);
                }
            }

            if (!obsoleteNames.isEmpty()) {
                log.error("CRITICAL GAP: Found permissions in DB that no longer exist in Enum code: {}. "+
                        "The will be purged to preserve system and bitmask consistency.", obsoleteNames);
                for (String obsolete : obsoleteNames) {
                    permissionRepository.deleteObsoletePermission(obsolete);
                }
            }

            if (!validNamesInDb.isEmpty()) {
                permissionRepository.findAllByPermissionNameIn(validNamesInDb).forEach(permission -> {
                    permissionsMap.put(permission.getPermissionName(), permission);
                    if (permission.getId() != null) { // Asumiendo que getId() devuelve el UUID de la entidad
                        uuidPermissionMap.put(permission.getId(), permission);
                    }
                });
            }

            Set<PermissionName> missingPermissions = Arrays.stream(PermissionName.values())
                    .filter(enumName -> !permissionsMap.containsKey(enumName))
                    .collect(Collectors.toSet());

            if (!missingPermissions.isEmpty()) {
                log.warn(
                        "Synchronization gap detected. Missing {} permissions in DB: {}",
                        missingPermissions.size(), missingPermissions
                );

                List<Permission> toPersist = missingPermissions.stream()
                        .map(enumName -> new Permission(
                                uuidService.next(),
                                enumName
                        )).toList();

                List<Permission> savedPermissions = permissionRepository.saveAll(toPersist);
                savedPermissions.forEach(permission -> {
                    permissionsMap.put(permission.getPermissionName(), permission);
                    if (permission.getId() != null) {
                        uuidPermissionMap.put(permission.getId(), permission);
                    }
                });
                log.info("Successfully synchronized and persisted missing permissions to DB.");
            } else {
                log.info("DB and Enum are synchronized. No deep structural sync required.");
            }

            log.info("Permissions cache initialized successfully with {} active permissions", permissionsMap.size());

        } catch (Exception e) {
            log.error("FATAL ERROR on starting/synchronizing PermissionsCache", e);
            throw new IllegalStateException("Failed to initialize system permissions. Core security compromised.", e);
        }
    }

    @Deprecated
    public void refresh() {
        // Bloqueamos sobre un objeto común o usamos un cerrojo para asegurar la consistencia al vaciar ambos mapas
        synchronized (permissionsMap) {
            permissionsMap.clear();
            uuidPermissionMap.clear();
            init();
        }
    }

    public Permission get(PermissionName permissionName) {
        return permissionsMap.computeIfAbsent(
                permissionName,
                name -> {
                    log.warn("Cache miss for permission name: {}. Searching it on DB...", name);
                    Permission permission = permissionRepository.findByPermissionName(name)
                            .orElseThrow(() -> new IllegalStateException("Permission: [%s] was not found.".formatted(name)));

                    // Si hubo un miss pero se encontró en DB, se sincroniza también el mapa de UUIDs
                    if (permission.getId() != null) {
                        uuidPermissionMap.put(permission.getId(), permission);
                    }
                    return permission;
                });
    }


    @NonNull
    public PermissionName getPermissionName(UUID permissionId) {
        Permission permission = uuidPermissionMap.get(permissionId);

        if (permission == null) {
            throw new IllegalStateException("Permission: [%s] was not found.".formatted(permissionId));
        }
        return permission.getPermissionName();
    }

    @NonNull
    public Permission get(UUID permissionId) {
        Permission permission = uuidPermissionMap.get(permissionId);

        if (permission == null) {
            throw new IllegalArgumentException("No permission found with id: '%s'.".formatted(permissionId));
        }

        return permission;
    }

    public Set<Permission> getAll() {
        return new HashSet<>(permissionsMap.values());
    }

    /**
     * Verifica si el conjunto de permisos proporcionado equivale al total de permisos
     * activos en el sistema (Enum actual sincronizado con DB).
     */
    public boolean hasAllActivePermissions(Collection<Permission> rolePermissions) {
        if (rolePermissions == null) return false;
        return rolePermissions.size() == permissionsMap.size();
    }


    public void checkExistById(@NotNull @NotEmpty Collection<@NotNull UUID> permissionIds) {
        if (!uuidPermissionMap.keySet().containsAll(permissionIds)){
            throw new IllegalStateException("At least one permission was not found or does not exist.");
        }
    }
}