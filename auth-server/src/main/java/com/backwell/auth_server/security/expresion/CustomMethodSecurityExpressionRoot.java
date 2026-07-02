package com.backwell.auth_server.security.expresion;

import com.backwell.auth_server.security.checker.SecurityChecker;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;


public class CustomMethodSecurityExpressionRoot extends SecurityExpressionRoot implements MethodSecurityExpressionOperations {
    private final SecurityChecker securityChecker;
    private Object filterObject;
    private Object returnObject;
    private Object targetObject;

    public CustomMethodSecurityExpressionRoot(Authentication authentication, SecurityChecker securityChecker) {
        super(authentication);
        this.securityChecker = securityChecker;
    }

    public boolean hasPermission(String permissionString) {
        return securityChecker.hasPermission(permissionString);
    }

    public boolean hasAllPermissions(String[] permissionsStr) {
        if (permissionsStr == null || permissionsStr.length == 0) {
            return false;
        }

        Set<String> permissionsSet = Arrays.stream(permissionsStr)
                .map(String::trim)
                .collect(Collectors.toSet());
        return securityChecker.hasPermissions(permissionsSet);
    }

    @Override public void setFilterObject(Object filterObject) { this.filterObject = filterObject; }
    @Override public Object getFilterObject() { return filterObject; }
    @Override public void setReturnObject(Object returnObject) { this.returnObject = returnObject; }
    @Override public Object getReturnObject() { return returnObject; }
    @Override public Object getThis() { return targetObject; }
}
