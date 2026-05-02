package com.backwell.auth_server.jpa.service;

import com.backwell.auth_server.dto.internal.OAuthGetOrCreateUserResult;
import com.backwell.auth_server.dto.request.CreateUserRequest;
import com.backwell.auth_server.dto.response.MessageResponse;
import com.backwell.auth_server.exception.AuthProviderMismatchException;
import com.backwell.auth_server.exception.UserAlreadyExistsException;
import com.backwell.auth_server.exception.role.LastOwnerExclusionException;
import com.backwell.auth_server.jpa.entity.Role;
import com.backwell.auth_server.jpa.entity.User;
import com.backwell.enums.AuthProvider;
import com.backwell.enums.RoleName;
import com.backwell.auth_server.jpa.repo.UserRepository;
import com.backwell.auth_server.service.UUIDGeneratorService;
import com.backwell.auth_server.jpa.registry.RoleRegistry;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class JpaUserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UUIDGeneratorService uuidService;
    private final RoleRegistry roleRegistry;

    @Transactional
    public MessageResponse createLocalUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            String msg = String.format("User with email `%s` already exists", request.email());
            throw new UserAlreadyExistsException(msg);
        }

        Role userRole = roleRegistry.get(RoleName.USER);

        User user = User.builder()
                .id(uuidService.generate())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .authProvider(AuthProvider.LOCAL)
                .roles(Set.of(userRole))
                .build();

        User savedUser = userRepository.save(user);
        String msg = String.format("App user with email `%s` created", savedUser.getEmail());
        return new MessageResponse(msg);
    }



    @Transactional
    public OAuthGetOrCreateUserResult getOrCreateOAuthUser(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BadRequestException("email is empty");
        }
        return userRepository.findByEmail(email)
                .map(user -> {
                    if (!user.getAuthProvider().equals(AuthProvider.GOOGLE)) {
                        throw new AuthProviderMismatchException("The CurrentUser has been registered with social login");
                    }
                    return new OAuthGetOrCreateUserResult(user, false);
                }).orElseGet(()-> {
                    User.UserBuilder builder = User.builder()
                            .id(uuidService.generate())
                            .email(email)
                            .password("{noop}password")
                            .authProvider(AuthProvider.GOOGLE);

                    Role role = roleRegistry.get(RoleName.USER);
                    builder.roles(Set.of(role));
                    User saved = userRepository.save(builder.build());
                    return new OAuthGetOrCreateUserResult(saved, true);
                });
    }

    @Transactional
    public MessageResponse grantRole(String email, RoleName targetedRole) {
        User user = findByEmail(email);

        Set<Role> roles = user.getRoles();
        roles.add(roleRegistry.get(targetedRole));

        userRepository.save(user);
        return new MessageResponse("Roles Updated Successfully");
    }

    @Transactional
    public MessageResponse revokeRole(String email, RoleName role, RoleName targetedRole) {
        if (!role.canRevoke(targetedRole)){
            return new MessageResponse("Insufficient Role");
        }

        if (role == RoleName.OWNER && role == targetedRole) {
            if (!userRepository.exitsAtLeastTwoWithRole(RoleName.OWNER)) {
                throw new LastOwnerExclusionException("Could not Revoke last OWNER User.System requires at least one OWNER User");
            }
        }

        User user = findByEmail(email);
        Set<Role> roles = user.getRoles();
        boolean removed = roles.remove(roleRegistry.get(targetedRole));
        String msg = removed ? "Roles Updated Successfully" : String.format("User `%s` had not such Role",  user.getEmail());
        return new MessageResponse(msg);
    }


    private User findByEmail(String email) {
        if (email == null || email.isEmpty()) {
            throw new IllegalArgumentException("Provided email is empty");
        }

        return userRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("User with email `" + email + "` not found"));
    }
}
