package com.backwell.api_service.modules.inventory.entity;

import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryTrace {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id",  nullable = false, updatable = false)
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
    @JdbcTypeCode(Types.TIMESTAMP_WITH_TIMEZONE)
    private Instant timestamp;

    @PrePersist
    protected void onCreate() {
        if (incomeLossArithmeticalTest() == transferArithmeticalTest()) {
            throw new SystemException("Arithmetical test failed. This movement is not consistent");
        }

        timestamp = Instant.now();
    }

    private boolean incomeLossArithmeticalTest(){
        return physicalDelta == availableDelta + reservedDelta + redundancyDelta;
    }

    private boolean transferArithmeticalTest(){
        return 0 == physicalDelta +  availableDelta + reservedDelta + redundancyDelta;
    }

    public UUID getItemId() {
        return item.getId();
    }
}
