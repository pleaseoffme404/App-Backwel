package com.backwell.auth_server.jpa.service;

import com.backwell.auth_server.dto.internal.OAuthGetOrCreateUserResult;
import com.backwell.auth_server.dto.request.CreateUserRequest;
import com.backwell.auth_server.dto.response.MessageResponse;
import com.backwell.auth_server.exception.AuthProviderMismatchException;
import com.backwell.auth_server.exception.UserAlreadyExistsException;
import com.backwell.auth_server.init.RequiredRolesCache;
import com.backwell.auth_server.jpa.entity.Role;
import com.backwell.auth_server.jpa.entity.User;
import com.backwell.auth_server.jpa.repo.UserRepository;
import com.backwell.auth_server.util.UUIDService;
import com.backwell.enums.AuthProvider;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Slf4j
public class JpaUserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final UUIDService uuidService;
    private final RequiredRolesCache rolesCache;

    @Transactional
    public MessageResponse createLocalUser(CreateUserRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            String msg = String.format("User with email `%s` already exists", request.email());
            throw new UserAlreadyExistsException(msg);
        }

        Role userRole = rolesCache.getUserReference();

        User user = User.builder()
                .id(uuidService.next())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .authProvider(AuthProvider.LOCAL)
                .role(userRole)
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
                    User newUser = User.builder()
                            .id(uuidService.next())
                            .email(email)
                            .password("{noop}password")
                            .role(rolesCache.getUserReference())
                            .authProvider(AuthProvider.GOOGLE)
                            .build();

                    User saved = userRepository.save(newUser);
                    return new OAuthGetOrCreateUserResult(saved, true);
                });
    }
}
