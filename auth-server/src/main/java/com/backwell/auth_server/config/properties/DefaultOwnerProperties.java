package com.backwell.auth_server.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.defaults.owner")
public record DefaultOwnerProperties (
        @DefaultValue("OWNER") String roleName,
        @DefaultValue("owner@backwel.com") String user,
        boolean generatePassword,
        @DefaultValue("12") Integer passwordLength,
        String staticPassword
) {

    public DefaultOwnerProperties {

        if (roleName == null) roleName = "OWNER";
        if (user == null) user = "owner@backwell.com";

        if (staticPassword != null && !staticPassword.isBlank()) {
            generatePassword = false;

            if (staticPassword.length() != 16) {
                throw new IllegalArgumentException("staticPassword must be exactly 16 characters. Length: " + staticPassword.length());
            }

            passwordLength = 16;

        } else {

            if (passwordLength == null) passwordLength = 12;
            if (passwordLength < 12) {
                throw new IllegalArgumentException("passwordLength must be at least 12. Current: " + passwordLength);
            }
        }
    }
    public boolean shouldGenerateRandomPassword() { return this.generatePassword; }
    public boolean hasStaticPassword() { return this.staticPassword != null && !this.staticPassword.isBlank(); }
}
