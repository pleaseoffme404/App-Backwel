package com.backwell.api_service.modules.users.entity.cupon;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.modules.users.entity.UserInfo;
import jakarta.persistence.*;
import lombok.*;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class Cupon {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CuponType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserInfo user;

    @Column(nullable = false)
    private BigDecimal decimalFactor;

    @Setter
    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false, updatable = false)
    private boolean stackable;

    @Setter
    private Instant usedAt;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    @Builder
    public Cupon(UUID id, String name, CuponType type, UserInfo user, BigDecimal decimalFactor, boolean stackable) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.user = user;
        this.decimalFactor = decimalFactor;
        this.active = true;
        this.stackable = stackable;
    }

}
