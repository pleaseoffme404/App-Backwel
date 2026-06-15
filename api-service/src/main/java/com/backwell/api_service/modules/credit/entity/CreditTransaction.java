package com.backwell.api_service.modules.credit.entity;

import com.backwell.api_service.modules.users.entity.UserInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class CreditTransaction {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY,  optional = false)
    @JoinColumn(name = "user_id",  nullable = false, updatable = false)
    private UserInfo userInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id", updatable = false, nullable = true)
    private UserInfo actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private CreditTransactionType type;

    @Column(precision = 12, scale = 2, nullable = false, updatable = false)
    private BigDecimal delta;

    @Column(nullable = false, updatable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public CreditTransaction(UUID id, UUID idempotencyKey, UserInfo userInfo, CreditTransactionType type, BigDecimal delta) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.userInfo = userInfo;
        this.type = type;
        this.delta = delta;
    }

    public CreditTransaction(UUID id, UUID idempotencyKey, UserInfo userInfo, UserInfo actor, CreditTransactionType type, BigDecimal delta) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.userInfo = userInfo;
        this.actor = actor;
        this.type = type;
        this.delta = delta;
    }
}
