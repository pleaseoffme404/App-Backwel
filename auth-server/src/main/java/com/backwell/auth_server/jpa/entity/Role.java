package com.backwell.auth_server.jpa.entity;

import com.backwell.enums.RoleName;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "role_seq ")
    @SequenceGenerator(
            name = "role_seq",
            sequenceName = "role_seq"
    )
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(unique = true, nullable = false,  updatable = false)
    private RoleName roleName;

    public Role (RoleName roleName) {
        this.roleName = roleName;
    }
}
