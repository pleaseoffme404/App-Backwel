package com.backwell.api_service.common.config.user.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Restricts access exclusively to users with {@code RoleName.OWNER}
 * privileges.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@PreAuthorize("@auth.hasAuthority(T(com.backwell.enums.RoleName).OWNER)")
public @interface OwnerLevel {
}