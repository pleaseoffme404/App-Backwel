package com.backwell.auth_server.security.user;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.Map;

@Data
@NoArgsConstructor
public class AppOidcUser implements OidcUser, IdentityContainer {
    private OidcUser oidcUser;
    private UserDTO userDto;
    private boolean isNew;

    public AppOidcUser(OidcUser oidcUser, UserDTO userDto,  boolean isNew) {
        if (userDto == null) {
            throw new IllegalArgumentException("UserDto cannot be null");
        }

        this.oidcUser = oidcUser;
        this.userDto = userDto;
        this.isNew = isNew;
    }

    @Override
    @NonNull
    public UserDTO getUserDTO() {
        return userDto;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oidcUser.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.fetchAuthorities();
    }

    @Override
    public String getName() {
        return userDto.uuid().toString();
    }

    @Override
    public Map<String, Object> getClaims() {
        return oidcUser.getClaims();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        return oidcUser.getUserInfo();
    }

    @Override
    public OidcIdToken getIdToken() {
        return oidcUser.getIdToken();
    }
}
