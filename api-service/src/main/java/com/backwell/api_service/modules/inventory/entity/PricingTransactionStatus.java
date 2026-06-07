package com.backwell.api_service.modules.inventory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class PricingTransactionStatus {
    @Id
    private UUID transactionId;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant createdAt;

    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant calculatedAt;

    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant indexedAt;
}
