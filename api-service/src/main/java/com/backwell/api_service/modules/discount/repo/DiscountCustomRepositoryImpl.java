package com.backwell.api_service.modules.discount.repo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Supplier;

import static com.backwell.api_service.jooq.generated.Tables.*;
import static org.jooq.impl.DSL.*;


@Repository
@RequiredArgsConstructor
@Slf4j
public class DiscountCustomRepositoryImpl implements DiscountCustomRepository {
    private final DSLContext dsl;
    private final Supplier<OffsetDateTime> utcNowSupplier;

    @Override
    public BigDecimal resolveDiscountForProduct(UUID productId) {

        return null;
    }

    @Override
    public BigDecimal resolveDiscountForItem(UUID itemId) {
        return null;
    }



    public UUID[] buildAncestryPath(UUID categoryId) {
        var C = CATEGORY;
        Name cteName = name("category_ancestry");

        CommonTableExpression<?> cte = cteName.as(
                select(C.ID, C.PARENT_ID)
                        .from(C)
                        .where(C.ID.eq(categoryId))
                        .unionAll(
                                select(C.ID, C.PARENT_ID)
                                        .from(C)
                                        .join(cteName).on(C.ID.eq(field(name("category_ancestry", "parent_id"), UUID.class)))
                        )
        );

        return dsl.withRecursive(cte)
                .select(field(name("category_ancestry", "id"), UUID.class))
                .from(cte)
                .fetchArray(field(name("category_ancestry", "id"), UUID.class));
    }
}