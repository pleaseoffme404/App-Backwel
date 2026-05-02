package com.backwell.api_service.modules.inventory.entity;

import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class InventoryTrace {
    @Id
    private UUID id;

    @ManyToOne()
    private Item item;

    @Column(updatable = false)
    private int physicalBalance;
    @Column(updatable = false)
    private int physicalDelta;

    @Column(updatable = false)
    private int availableBalance;
    @Column(updatable = false)
    private int availableDelta;

    @Column(updatable = false)
    private int redundancyBalance;
    @Column(updatable = false)
    private int redundancyDelta;

    @Column(updatable = false)
    private int reservedBalance;
    @Column(updatable = false)
    private int reservedDelta;

    @Column(updatable = false)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (incomeLossArithmeticalTest() == transferArithmeticalTest()) throw new IllegalArgumentException("Arithmetical test failed. This movement is not consistent");
        timestamp = Instant.now();
    }

    private boolean incomeLossArithmeticalTest(){
        return physicalDelta == availableDelta + reservedDelta + redundancyDelta;
    }

    private boolean transferArithmeticalTest(){
        return 0 == physicalDelta +  availableDelta + reservedDelta + redundancyDelta;
    }
}
