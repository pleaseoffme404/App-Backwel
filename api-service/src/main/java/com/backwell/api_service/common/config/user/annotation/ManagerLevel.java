package com.backwell.api_service.common.config.user.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Restricts access to users with at least {@code RoleName.MANAGER} privileges.
 *
 * <p>
 * Users holding ADMIN or OWNER privileges are also authorized.
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@PreAuthorize("@auth.hasAuthority(T(com.backwell.enums.RoleName).MANAGER)")
public @interface ManagerLevel {
}