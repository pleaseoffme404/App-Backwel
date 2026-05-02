package com.backwell.auth_server.jpa.registry;

import com.backwell.auth_server.jpa.entity.Role;
import com.backwell.enums.RoleName;
import com.backwell.auth_server.jpa.repo.RoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@Slf4j
public class RoleRegistry {
    private final RoleRepository roleRepository;
    private final Map<RoleName, Role> roleCache = new EnumMap<>(RoleName.class);

    public RoleRegistry(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
        this.initialize();
    }

    private void initialize() {
        log.info("Cargando caché de roles desde la base de datos...");

        roleRepository.findAll().forEach(role -> {
            roleCache.put(role.getRoleName(), role);
        });

        if (roleCache.isEmpty()) {
            log.warn("La caché de roles está vacía. Esto podría causar fallos en los Bootstrappers.");
        } else {
            log.info("Registro de roles cargado exitosamente: {}", roleCache.keySet());
        }
    }

    public void refresh() {
        synchronized (roleCache) {
            roleCache.clear();
            initialize();
        }
    }

    public Role get(RoleName roleName) {
        Role role = roleCache.get(roleName);
        if (role == null) {
            return roleRepository.findByRoleName(roleName)
                    .map(r -> {
                        roleCache.put(roleName, r);
                        return r;
                    })
                    .orElseThrow(() -> new IllegalStateException("Error Crítico: El rol `" + roleName + "` no existe en la base de datos."));
        }
        return role;
    }
}