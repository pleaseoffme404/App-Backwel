package com.backwell.api_service.modules.products.jooq.impl;

import com.backwell.api_service.modules.products.jooq.dto.ItemAttributeTupleProjection;
import com.backwell.api_service.modules.products.jooq.repo.ItemAttributeCustomRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record2;
import org.jooq.Result;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import static org.jooq.impl.DSL.*;
import static com.backwell.api_service.jooq.generated.Tables.*;

@Repository
@RequiredArgsConstructor
public class ItemAttributeRepositoryImpl implements ItemAttributeCustomRepository {
    private final DSLContext c;

    @Override
    public Map<UUID, List<ItemAttributeTupleProjection>> mapItemAttributes(Collection<UUID> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Map.of();
        }

        var a = ITEM_ATTRIBUTE;
        var pa = PRODUCT_ATTRIBUTE;

        var innerA = ITEM_ATTRIBUTE.as("inner_a");

        return c.select(
                        a.ITEM_ID,
                        multiset(
                                select(
                                        pa.ATTRIBUTE_KEY,
                                        innerA.ATTRIBUTE_VALUE
                                )
                                        .from(innerA)
                                        .join(pa).on(pa.ID.eq(innerA.ATTRIBUTE_ID))
                                        .where(innerA.ITEM_ID.eq(a.ITEM_ID))
                        ).convertFrom(toItemAttributeTupleProjection)
                )
                .from(a)
                .where(a.ITEM_ID.in(itemIds))
                .fetchMap(
                        Record2::value1,
                        Record2::value2
                );
    }

    private final Function<Result<Record2<String, String>>, List<ItemAttributeTupleProjection>> toItemAttributeTupleProjection =
            r -> r.into(ItemAttributeTupleProjection.class);
}
