package com.backwell.api_service.modules.inventory.jooq.impl;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.modules.inventory.dto.ItemPricingDTO;
import com.backwell.api_service.modules.inventory.jooq.repo.PriceCalculationHistoryCustomRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.backwell.api_service.common.exception.codes.ItemErrorCode.ITEM_NOT_FOUND;
import static com.backwell.api_service.jooq.generated.Tables.PRICE_CALCULATION_HISTORY;
import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class PriceCalculationHistoryRepositoryImpl implements PriceCalculationHistoryCustomRepository {
    private final DSLContext c;

    @Override
    public ItemPricingDTO getForItem(UUID itemId) {
        var h = PRICE_CALCULATION_HISTORY;

        ItemPricingDTO result = c.select(
                        h.ITEM_ID,
                        h.BASE_PRICE,
                        h.FINAL_PRICE.as("current_price"),
                        h.DISCOUNT_DECIMAL
                )
                .from(h)
                .where(h.ITEM_ID.eq(itemId))
                .orderBy(h.CREATED_AT.desc())
                .limit(1)
                .fetchOneInto(ItemPricingDTO.class);

        if (result == null) {
            String msg = String.format("Requested item with Id: [%s] ", itemId);
            throw new BusinessException(msg, ITEM_NOT_FOUND);
        }
        return result;
    }

    @Override
    public Map<UUID, ItemPricingDTO> getForItems(Set<UUID> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        var h = PRICE_CALCULATION_HISTORY.as("h");

        Table<?> lastPrice = lateral(
                select(
                        h.BASE_PRICE,
                        h.FINAL_PRICE,
                        h.DISCOUNT_DECIMAL
                )
                        .from(h)
                        .where(h.ITEM_ID.eq(field(name("items", "item_id"), UUID.class)))
                        .orderBy(h.CREATED_AT.desc())
                        .limit(1)
        ).as("lp", "base_price", "final_price", "discount_decimal");

        @SuppressWarnings("unchecked")
        Row1<UUID>[] rows = itemIds.stream()
                .map(id -> (Row1<UUID>) row(id))
                .toArray(Row1[]::new);

        var itemRows = values(rows).asTable("items", "item_id");

        Field<UUID> itemId = itemRows.field("item_id", UUID.class);
        Field<BigDecimal> basePrice = field(name("lp", "base_price"), BigDecimal.class);
        Field<BigDecimal> finalPrice = field(name("lp", "final_price"), BigDecimal.class);
        Field<BigDecimal> discount = field(name("lp", "discount_decimal"), BigDecimal.class);

        Map<UUID, ItemPricingDTO> result = c
                .select(
                        itemId,
                        basePrice,
                        finalPrice.as("current_price"),
                        discount
                )
                .from(itemRows)
                .crossJoin(lastPrice)
                .fetchMap(itemId, ItemPricingDTO.class);

        // ── Validaciones ──────────────────────────────────────────────
        String joinedUUIDs = itemIds.stream()
                .map(UUID::toString)
                .collect(Collectors.joining(", "));

        if (result.isEmpty()) {
            throw new BusinessException(
                    String.format("Requested items with IDs: [%s] were not found", joinedUUIDs),
                    ITEM_NOT_FOUND
            );
        }
        if (!result.keySet().equals(itemIds)) {
            throw new BusinessException(
                    String.format("Some of the requested items with IDs: [%s] were not found", joinedUUIDs),
                    ITEM_NOT_FOUND
            );
        }

        return result;
    }
}
