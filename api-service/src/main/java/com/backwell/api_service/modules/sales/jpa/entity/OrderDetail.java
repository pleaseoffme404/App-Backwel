package com.backwell.api_service.modules.sales.jpa.entity;

import com.backwell.api_service.modules.users.entity.UserInfo;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Entity
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@Builder
public class OrderDetail {
    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    private UUID id;

    @Column(nullable = false, updatable = false)
    private UUID checkoutToken;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id",  nullable = false, updatable = false)
    private UserInfo user;

    @OneToMany(
            mappedBy = "order",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(
            mappedBy = "order",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<OrderTimeline> timeline = new ArrayList<>();

    @OneToMany(
            mappedBy = "order",
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<OrderAdjustment> adjustments = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    // public constructor


    public OrderDetail(UUID id, UUID checkoutToken, UserInfo user) {
        this.id = id;
        this.checkoutToken = checkoutToken;
        this.user = user;
    }

    // collection attributes helpers
    public void addOrderItems(Collection<OrderItem> items) {
        if (items == null || items.isEmpty()) return;

        items.forEach(this::addOrderItem);
    }
    public void addOrderItem(OrderItem orderItem) {
        if(orderItem == null) return;

        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }
    public void removeOrderItem(OrderItem orderItem) {
        if (orderItem == null) return;

        if (orderItems.remove(orderItem)) {
            orderItem.setOrder(null);
        }
    }

    public void addOrderTimelines(Collection<OrderTimeline> timelines) {
        if (timelines == null || timelines.isEmpty()) return;

        timelines.forEach(this::addOrderTimeline);
    }
    public void addOrderTimeline(OrderTimeline orderTimeline) {
        if (orderTimeline == null) return;

        timeline.add(orderTimeline);
        orderTimeline.setOrder(this);
    }
    public void removeOrderTimeline(OrderTimeline orderTimeline) {
        if (orderTimeline == null) return;

        if (timeline.remove(orderTimeline)) {
            orderTimeline.setOrder(null);
        }
    }

    public void addOrderAdjustments(Collection<OrderAdjustment> adjustments) {
        if (adjustments == null || adjustments.isEmpty()) return;

        adjustments.forEach(this::addOrderAdjustment);
    }
    public void addOrderAdjustment(OrderAdjustment orderAdjustment) {
        if (orderAdjustment == null) return;

        adjustments.add(orderAdjustment);
        orderAdjustment.setOrder(this);
    }
    public void removeOrderAdjustment(OrderAdjustment orderAdjustment) {
        if (orderAdjustment == null) return;

        if(adjustments.remove(orderAdjustment)) {
            orderAdjustment.setOrder(null);
        }
    }
}
