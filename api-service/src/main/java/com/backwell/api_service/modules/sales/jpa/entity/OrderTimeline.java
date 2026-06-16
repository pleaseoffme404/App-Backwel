package com.backwell.api_service.modules.sales.jpa.entity;

import com.backwell.api_service.modules.sales.jpa.enums.OrderTimelineEvent;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Entity
public class OrderTimeline {
    @Id
    UUID id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private OrderDetail order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    OrderTimelineEvent event;

    @Column(columnDefinition = "TEXT")
    private String additionalInfo;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMPTZ DEFAULT NOW()")
    private Instant timestamp;

    @PrePersist()
    protected void onCreate() {
        this.timestamp = Instant.now();
    }

    public OrderTimeline(UUID id, OrderTimelineEvent event, String additionalInfo) {
        this.id = id;
        this.event = event;
        this.additionalInfo = additionalInfo;
    }
}
