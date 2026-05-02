package com.backwell.auth_server.security.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppUserDetails implements UserDetails, IdentityContainer {
    private UserDTO user;

    @Override
    public UserDTO getUserDTO() {
        return user;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !user.expired();
    }

    @Override
    public boolean isAccountNonLocked() {
        return !user.locked();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return !user.credentialsExpired();
    }

    @Override
    public boolean isEnabled() {
        return !user.disabled();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.roles().stream().map(SimpleGrantedAuthority::new).collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.password();
    }

    @Override
    public String getUsername() {
        return user.uuid().toString();
    }
}
