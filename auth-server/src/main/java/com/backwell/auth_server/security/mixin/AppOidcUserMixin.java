package com.backwell.auth_server.security.mixin;


import com.backwell.auth_server.security.user.UserDTO;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
public abstract class AppOidcUserMixin {

    @JsonDeserialize(as = OidcUser.class)
    abstract OidcUser getOidcUser();

    abstract UserDTO getUserDto();

    abstract boolean isNew();
}
