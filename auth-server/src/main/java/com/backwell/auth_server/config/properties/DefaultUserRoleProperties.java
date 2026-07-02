package com.backwell.auth_server.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.defaults.user-role")
public record DefaultUserRoleProperties( @DefaultValue("USER") String name) { }
