package com.backwell.api_service.modules.products.jpa.entity.prod;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ItemPicture {
    @Id
    @Setter(AccessLevel.NONE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false, updatable = false)
    private Item item;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    /**
     * Starting at 0 for first image*/
    @Column(name = "image_order")
    private Integer order;
}
