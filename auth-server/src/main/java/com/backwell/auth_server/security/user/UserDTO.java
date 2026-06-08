package com.backwell.auth_server.security.user;

import com.backwell.auth_server.jpa.entity.User;
import com.backwell.enums.AuthProvider;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public record UserDTO(
        UUID uuid,
        String email,
        String password,
        AuthProvider authProvider,
        Set<String> roles,
        boolean expired,
        boolean locked,
        boolean credentialsExpired,
        boolean disabled
) {
    public static UserDTO fromEntity(User e) {
        Set<String> roles = e.getRoles()
                .stream()
                .map(r-> r.getRoleName().name())
                .collect(Collectors.toSet());

        return new UserDTO(
                e.getId(),
                e.getEmail(),
                e.getPassword(),
                e.getAuthProvider(),
                roles,
                e.isExpired(),
                e.isLocked(),
                e.isCredentialsExpired(),
                e.isDisabled()
        );
    }
}
