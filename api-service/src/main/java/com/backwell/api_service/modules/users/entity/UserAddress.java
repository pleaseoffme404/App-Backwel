package com.backwell.api_service.modules.users.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_address_seq")
    @SequenceGenerator(name = "user_address_seq", sequenceName = "user_address_seq", allocationSize = 50)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserInfo user;

    @Column(nullable = false)
    private int slotIndex;

    @Column(length = 100, nullable = false)
    private String internalName;

    @Embedded
    private GoogleAddress googleAddress;

    public UserAddress(int slotIndex, String internalName, GoogleAddress googleAddress) {
        this.slotIndex = slotIndex;
        this.internalName = internalName;
        this.googleAddress = googleAddress;
    }
}
