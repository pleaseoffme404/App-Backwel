package com.backwell.auth_server.init;


import com.backwell.auth_server.jpa.entity.Permission;
import com.backwell.auth_server.jpa.repo.PermissionRepository;
import com.backwell.auth_server.util.UUIDService;
import com.backwell.enums.PermissionName;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class PermissionsInitializer implements ApplicationInitializer {
    private final PermissionRepository permissionRepository;
    private final UUIDService uuidService;

    @Override
    public void initialize() {
        log.info("Running Permissions Initializer...");
        // check or insert operation
        List<Permission> savedPermissions = permissionRepository.findAll();

        PermissionName[] expectedValues = PermissionName.values();

        if (savedPermissions.isEmpty()) {
            // insert all permissions
            log.info("Found 0 permissions in database. Injecting declared permissions...");
            List<Permission> newPermissions = Arrays.stream(expectedValues)
                    .map(p-> new Permission(
                            uuidService.next(),
                            p
                    )).toList();
            int inserted = permissionRepository.saveAll(newPermissions).size();
            log.info("Inserted {} permissions to database.", inserted);
            return;
        }

        if (savedPermissions.size() != expectedValues.length) {
            String msg = String.format("Expected %d permissions but found %d", expectedValues.length, savedPermissions.size());
            log.error(msg);
            throw new IllegalArgumentException(msg);
        }

        log.info("Found {} permissions in database.", savedPermissions.size());
        log.info("Permissions check resolved successfully.");
    }
}
