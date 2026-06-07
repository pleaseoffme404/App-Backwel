package com.backwell.api_service.modules.inventory.service;

import com.backwell.api_service.modules.products.meilisearch.dto.IndexableProductDTO;
import com.backwell.api_service.modules.products.meilisearch.service.AsyncSearchIndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PriceStreamProcessor {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final AsyncSearchIndexingService asyncSearchIndexingService;

    @Transactional(readOnly = true)
    public void streamAndIndexPrices(UUID transactionId) {
        MapSqlParameterSource paramSource = new MapSqlParameterSource();
        paramSource.addValue("transaction_id", transactionId);

        String streamQuery = """
                SELECT
                    item_id,
                    base_price,
                    final_price,
                    discount_decimal
                FROM price_calculation_history h
                WHERE transaction_id = :transaction_id""";

        jdbcTemplate.queryForStream(
                streamQuery,
                paramSource,
                (rs, rowNum) -> new ItemPriceDTO(
                        rs.getObject("item_id", UUID.class),
                        rs.getBigDecimal("base_price"),
                        rs.getBigDecimal("final_price"),
                        rs.getBigDecimal("discount_decimal")
                )
        ).forEach(i-> {
                    boolean hasDiscount = true;
                    BigDecimal discountPercentage = i.discountDecimal.multiply(BigDecimal.valueOf(100));

                    if (i.discountDecimal.compareTo(BigDecimal.ONE) >= 0){
                        hasDiscount = false;
                        discountPercentage = BigDecimal.ZERO;
                    }

                    asyncSearchIndexingService.addToIndexBuffer(
                            IndexableProductDTO.builder()
                                    .id(i.itemId)
                                    .transactionId(transactionId)
                                    .basePrice(i.basePrice)
                                    .currentPrice(hasDiscount ? i.finalPrice : i.basePrice)
                                    .hasDiscount(hasDiscount)
                                    .discountPercentage(discountPercentage)
                                    .build()
                    );
                }
        );
    }

    private record ItemPriceDTO(
            UUID itemId,
            BigDecimal basePrice,
            BigDecimal finalPrice,
            BigDecimal discountDecimal
    ) { }
}
