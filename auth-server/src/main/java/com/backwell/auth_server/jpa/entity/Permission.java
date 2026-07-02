package com.backwell.auth_server.jpa.entity;

import com.backwell.enums.PermissionName;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
public class Permission {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false, updatable = false)
    private PermissionName permissionName;

    public Permission(UUID id, PermissionName permissionName) {
        Objects.requireNonNull(id, "Id can't be null");
        Objects.requireNonNull(permissionName, "PermissionName can't be null");

        this.id = id;
        this.permissionName = permissionName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, permissionName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        Permission other = (Permission) obj;

        return Objects.equals(this.getId(), other.getId()) &&
                Objects.equals(this.getPermissionName(), other.getPermissionName());
    }


    /**
     * Same as effecting getPermissionName().getValue()*/
    @Deprecated
    public String getPermissionString() {
        return permissionName.getValue();
    }


    /**
     * Returns a front friendly string of the RoleName value*/
    public String getPermissionNameString() {
        return permissionName.name()
                .replace("_", " ");

    }
}
