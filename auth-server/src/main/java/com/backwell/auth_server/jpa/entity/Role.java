package com.backwell.auth_server.jpa.entity;

import com.backwell.enums.PermissionName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@NoArgsConstructor
@Data
@Builder
@AllArgsConstructor
public class Role {
    @Id
    private UUID id;

    @Column(nullable = false,  unique = true)
    private String name;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id", nullable = false, updatable = false),
            inverseJoinColumns = @JoinColumn(name = "permission_id", nullable = false, updatable = false)
    )
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Permission> permissions = new HashSet<>();

    public Set<PermissionName> getPermissionNamesSet() {
        return getPermissions().stream()
                .map(Permission::getPermissionName)
                .collect(Collectors.toSet());
    }
}