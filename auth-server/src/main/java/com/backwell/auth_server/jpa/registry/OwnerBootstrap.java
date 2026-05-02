package com.backwell.auth_server.jpa.registry;

import com.backwell.auth_server.jpa.entity.User;
import com.backwell.enums.AuthProvider;
import com.backwell.enums.RoleName;
import com.backwell.auth_server.jpa.repo.UserRepository;
import com.backwell.auth_server.service.UUIDGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class OwnerBootstrap implements CommandLineRunner {
    private final UserRepository userRepository;
    private final RoleRegistry roleRegistry;
    private final PasswordEncoder passwordEncoder;
    private final UUIDGeneratorService uuidService;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.existsUserWithRole(RoleName.OWNER)){
            log.info("OWNER User Found");
            return;
        }
        log.warn("OWNER User Not Found. Creating new OWNER User");
        User owner = User.builder()
                .id(uuidService.generate())
                .email("admin@backwell.com")
                .password(passwordEncoder.encode("admin123"))
                .authProvider(AuthProvider.LOCAL)
                .roles(Set.of(roleRegistry.get(RoleName.OWNER)))
                .build();

        userRepository.save(owner);
        log.info("##########################################################");
        log.info("#  OWNER INICIAL CREADO: admin@backwell.com              #");
        log.info("##########################################################");
    }
}
