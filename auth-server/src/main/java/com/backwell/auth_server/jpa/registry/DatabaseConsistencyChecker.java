package com.backwell.auth_server.jpa.registry;

import com.backwell.enums.RoleName;
import com.backwell.auth_server.jpa.repo.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)
public class DatabaseConsistencyChecker implements CommandLineRunner {
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        verifyRolesConsistency();
    }

    private void verifyRolesConsistency() throws IllegalStateException {
        log.info("Starting Role Integrity Checker");
        Set<RoleName> requiredRoles = Set.of(RoleName.values());
        long existingRolesCount = roleRepository.countByRoleNameIn(requiredRoles);

        if (existingRolesCount != requiredRoles.size()) {
            String msg = String.format("Expected %d Roles. Found %d", requiredRoles.size(), existingRolesCount);
            log.error(msg);
            throw new IllegalStateException(msg);
        }

        log.info("Completed Role Integrity Check");
    }
}
