package com.backwell.api_service.modules.users.entity.credit;

import com.backwell.api_service.modules.users.entity.UserInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class CreditTransaction {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY,  optional = false)
    @JoinColumn(name = "user_id",  nullable = false)
    private UserInfo userInfo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CreditTransactionType type;

    @Column(nullable = false, updatable = false)
    private BigDecimal delta;


    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public CreditTransaction(UUID id, UserInfo userInfo, CreditTransactionType type, BigDecimal delta) {
        this.id = id;
        this.userInfo = userInfo;
        this.type = type;
        this.delta = delta;
    }
}
