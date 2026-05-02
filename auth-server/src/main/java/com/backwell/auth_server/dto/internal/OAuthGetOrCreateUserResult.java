package com.backwell.auth_server.dto.internal;
import com.backwell.auth_server.jpa.entity.User;

public record OAuthGetOrCreateUserResult(
        User user,
        boolean isNew
) {
}
