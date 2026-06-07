package com.backwell.api_service.modules.inventory.jooq.impl;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.modules.inventory.dto.ItemPricingDTO;
import com.backwell.api_service.modules.inventory.jooq.repo.PriceCalculationHistoryCustomRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

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

        var h = PRICE_CALCULATION_HISTORY;
        
        var rankedHistory = name("ranked_history").as(
                select(
                        h.ITEM_ID,
                        h.BASE_PRICE,
                        h.FINAL_PRICE,
                        h.DISCOUNT_DECIMAL,
                        rowNumber()
                                .over(partitionBy(h.ITEM_ID)
                                        .orderBy(h.CREATED_AT.desc()))
                                .as("rn")
                )
                        .from(h)
                        .where(h.ITEM_ID.in(itemIds))
        );

        // 2. Ejecutamos la consulta principal usando la CTE
        Map<UUID, ItemPricingDTO> result = c.with(rankedHistory)
                .select(
                        rankedHistory.field(h.ITEM_ID),
                        rankedHistory.field(h.BASE_PRICE),
                        rankedHistory.field(h.FINAL_PRICE).as("current_price"),
                        rankedHistory.field(h.DISCOUNT_DECIMAL)
                )
                .from(rankedHistory)
                .where(rankedHistory.field("rn", Integer.class).eq(1))
                
                .fetchMap(
                        rankedHistory.field(h.ITEM_ID),
                        ItemPricingDTO.class
                );

        String joinedUUIDs = itemIds.stream().map(UUID::toString).collect(Collectors.joining(", "));
        if (result.isEmpty()) {
            String msg = String.format("Requested items with IDs: [%s] were not found", joinedUUIDs);
            throw new BusinessException(msg, ITEM_NOT_FOUND);
        }

        if (result.size() != itemIds.size() || !result.keySet().equals(itemIds)) {
            String msg = String.format("Some of the requested items with IDs: [ %s ] were not found",  joinedUUIDs);
            throw new BusinessException(msg, ITEM_NOT_FOUND);
        }

        return result;
    }
}
