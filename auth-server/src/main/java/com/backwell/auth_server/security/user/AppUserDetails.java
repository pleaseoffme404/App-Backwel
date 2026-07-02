package com.backwell.auth_server.security.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Data
@NoArgsConstructor
public class AppUserDetails implements UserDetails, IdentityContainer {
    private UserDTO user;

    public AppUserDetails(UserDTO user) {
        if (user == null) {
            throw new IllegalArgumentException("UserDTO cannot be null");
        }
        this.user = user;
    }

    @Override
    @NonNull
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
        return this.fetchAuthorities();
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
