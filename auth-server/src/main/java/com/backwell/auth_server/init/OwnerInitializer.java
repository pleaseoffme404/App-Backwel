package com.backwell.auth_server.init;

import com.backwell.auth_server.config.properties.DefaultOwnerProperties;
import com.backwell.auth_server.jpa.entity.Role;
import com.backwell.auth_server.jpa.entity.User;
import com.backwell.auth_server.jpa.repo.UserRepository;
import com.backwell.auth_server.util.RandomPasswordGenerator;
import com.backwell.auth_server.util.UUIDService;
import com.backwell.enums.AuthProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Initializer responsible for setting up and synchronizing the system's root
 * {@code OWNER} role and master user account during application startup.
 * <p>This component executes sequentially as part of the application initialization
 * phase (ordered at position 4, right after RequiredRolesCache). It ensures that:
 * <ol>
 * <li>A master root account possessing the OWNER role is present in the database.</li>
 * </ol>
 * @see ApplicationInitializer
 * @see DefaultOwnerProperties
 * @see RequiredRolesCache
 */
@Component
@Slf4j
@RequiredArgsConstructor
@Order(4)
public class OwnerInitializer implements ApplicationInitializer {

    private final RequiredRolesCache requiredRolesCache;
    private final UUIDService uuidService;
    private final UserRepository userRepository;
    private final DefaultOwnerProperties ownerProperties;
    private final RandomPasswordGenerator passwordGenerator;
    private final PasswordEncoder passwordEncoder;

    /**
     * Executes the initialization logic for the master owner user entity.
     * This operation is wrapped in a transaction to ensure atomicity during setup.
     * * @throws IllegalStateException if a data integrity violation occurs, such as
     * an existing username conflicting with the root configuration.
     */
    @Override
    @Transactional
    public void initialize() {
        Role ownerRoleProxy = requiredRolesCache.getOwnerReference();
        computeOwnerUser(ownerRoleProxy);
    }

    /**
     * Checks for the existence of an active root user linked to the provided owner role.
     * If no such user exists, a new master owner account is bootstrapped.
     * * <p>Depending on the configuration properties, the password will either be statically
     * assigned or randomly generated and dumped securely to the console logs.
     * * @param ownerRole the lazy proxy reference representing the root role.
     * @throws IllegalStateException if the target username already exists with a different role.
     */
    private void computeOwnerUser(Role ownerRole) {
        UUID ownerRoleId = ownerRole.getId();
        String targetRoleName = ownerProperties.roleName();

        if (userRepository.existsUsersWithRole(ownerRoleId)) {
            log.info("An active user with the role '{}' was found. Root account requirement satisfied.", targetRoleName);
            return;
        }

        log.warn("No user with role '{}' found. Initializing master OWNER account...", targetRoleName);
        String rawPassword = ownerProperties.shouldGenerateRandomPassword()
                ? passwordGenerator.generatePassword(ownerProperties.passwordLength())
                : ownerProperties.staticPassword();

        try {
            User defaultOwner = User.builder()
                    .id(uuidService.next())
                    .email(ownerProperties.user())
                    .password(passwordEncoder.encode(rawPassword))
                    .authProvider(AuthProvider.LOCAL)
                    .role(ownerRole)
                    .build();

            userRepository.save(defaultOwner);

            log.info(
                    "\n{}\n  MASTER OWNER ACCOUNT CREATED SUCCESSFULLY\n  Username: {}\n  Password: {}\n{}",
                    "=".repeat(80),
                    ownerProperties.user(),
                    ownerProperties.shouldGenerateRandomPassword() ? rawPassword : "[STATIC]",
                    "=".repeat(80)
            );
        } catch (DataIntegrityViolationException e) {
            String msg = "Cannot create OWNER user '%s': username exists with conflicting role.".formatted(ownerProperties.user());
            log.error(msg);
            throw new IllegalStateException(msg, e);
        }
    }
}