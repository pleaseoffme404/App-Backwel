package com.backwell.api_service.modules.sales.jpa.entity;

import com.backwell.api_service.modules.users.entity.UserInfo;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class PointOfSaleOrder {
    @Id
    private UUID orderId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false,  updatable = false)
    private OrderDetail orderDetail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_user_id")
    private UserInfo sellerUser;
}
