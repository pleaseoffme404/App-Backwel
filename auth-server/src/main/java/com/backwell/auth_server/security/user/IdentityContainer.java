package com.backwell.auth_server.security.user;

import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;

public interface IdentityContainer {
    @NonNull
    UserDTO getUserDTO();


    default Collection<? extends GrantedAuthority> fetchAuthorities() {
        return getUserDTO().permissionNames().stream()
                .map(permissionName -> new SimpleGrantedAuthority(
                        permissionName.getValue()
                )).toList();
    }
}
