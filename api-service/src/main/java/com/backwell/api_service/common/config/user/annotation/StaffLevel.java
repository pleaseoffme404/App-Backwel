package com.backwell.api_service.common.config.user.annotation;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Restricts access to users with at least {@code RoleName.STAFF} privileges.
 *
 * <p>
 * Because role evaluation is hierarchical, users with higher-level roles
 * (for example, MANAGER, ADMIN, or OWNER) are also granted access.
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@PreAuthorize("@auth.hasAuthority(T(com.backwell.enums.RoleName).STAFF)")
public @interface StaffLevel {
}