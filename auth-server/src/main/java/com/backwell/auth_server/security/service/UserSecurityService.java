package com.backwell.auth_server.security.service;

import com.backwell.auth_server.dto.internal.OAuthGetOrCreateUserResult;
import com.backwell.auth_server.jpa.entity.User;
import com.backwell.enums.AuthProvider;
import com.backwell.auth_server.jpa.repo.UserRepository;
import com.backwell.auth_server.jpa.service.JpaUserService;
import com.backwell.auth_server.security.user.AppOidcUser;
import com.backwell.auth_server.security.user.AppUserDetails;
import com.backwell.auth_server.security.user.UserDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSecurityService extends OidcUserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final JpaUserService jpaUserService;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user;

        try {
            UUID uuid = UUID.fromString(username);
            log.info("loadUserByUsername uuid:{}", uuid);
            user = userRepository.findById(uuid).orElse(null);
        } catch (IllegalArgumentException e) {
            //If the username was not a UUID
            log.info("loadUserByUsername email:{}", username);
            user = userRepository.findByEmail(username)
                    .orElseThrow(()-> new UsernameNotFoundException(username));
        }

        if (user == null) {
            throw new UsernameNotFoundException(username);
        }

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            String msg = String.format(
                    "This user has already been registered using `%s` auth provider",
                    user.getAuthProvider()
            );
            throw new UsernameNotFoundException(msg);
        }

        return new AppUserDetails(UserDTO.fromEntity(user));
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String email = oidcUser.getAttribute("email");

        OAuthGetOrCreateUserResult r = jpaUserService.getOrCreateOAuthUser(email);
        Set<GrantedAuthority> mappedAuthorities = new HashSet<>(oidcUser.getAuthorities());
        mappedAuthorities.add(new SimpleGrantedAuthority("SCOPE_offline_access"));
        var scopePatchedUser = new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken(), oidcUser.getUserInfo());

        return new AppOidcUser(scopePatchedUser, UserDTO.fromEntity(r.user()), r.isNew());
    }
}
