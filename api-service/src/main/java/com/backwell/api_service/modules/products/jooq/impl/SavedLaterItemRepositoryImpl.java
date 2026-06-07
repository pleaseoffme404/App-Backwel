package com.backwell.api_service.modules.products.jooq.impl;

import com.backwell.api_service.modules.products.dto.SavedItemExtract;
import com.backwell.api_service.modules.products.jooq.repo.SavedLaterItemCustomRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


import static com.backwell.api_service.jooq.generated.Tables.*;
import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class SavedLaterItemRepositoryImpl implements SavedLaterItemCustomRepository {
    private final DSLContext c;


    @Override
    public List<SavedItemExtract> getSavedItemsExtract(UUID userUuid) {
        var si = SAVED_LATER_ITEM;
        var list = SAVED_LATER_LIST;
        var i = ITEM;
        var p = PRODUCT;
        var pic = ITEM_PICTURE;
        var h = PRICE_CALCULATION_HISTORY;

        Table<?> picLateral;
        Table<?> lhLateral;

        CommonTableExpression<Record1<UUID>> savedItemIds = name("saved_item_ids")
                .fields("item_id")
                .asMaterialized(
                        select(si.ITEM_ID)
                                .from(si)
                                .join(list).on(list.ID.eq(si.LIST_ID))
                                .where(list.USER_ID.eq(userUuid))
                );

        Field<UUID> savedItemIdRef = field(
                name("saved_item_ids", "item_id"), UUID.class
        );

        picLateral = lateral(
                select(pic.URL)
                        .from(pic)
                        .where(pic.ITEM_ID.eq(savedItemIdRef))
                        .orderBy(pic.IMAGE_ORDER.asc())
                        .limit(1)
        ).as("pic", "url");

        lhLateral = lateral(
                select(h.BASE_PRICE, h.FINAL_PRICE, h.DISCOUNT_DECIMAL)
                        .from(h)
                        .where(h.ITEM_ID.eq(savedItemIdRef))
                        .orderBy(h.CREATED_AT.desc())
                        .limit(1)
        ).as("lh", "base_price", "final_price", "discount_decimal");

        return c
                .with(savedItemIds)
                .select(
                        savedItemIds.field("item_id",UUID.class),
                        p.NAME,
                        field(name("pic", "url"), String.class),
                        i.SKU,
                        field(name("lh", "base_price"), BigDecimal.class),
                        field(name("lh", "final_price"), BigDecimal.class).as("current_price"),
                        field(name("lh", "discount_decimal"), BigDecimal.class)
                )
                .from(savedItemIds)
                .join(i).on(i.ID.eq(savedItemIds.field("item_id", UUID.class)))
                .join(p).on(p.ID.eq(i.PRODUCT_ID))
                .join(picLateral).on(trueCondition())
                .join(lhLateral).on(trueCondition())
                .fetchInto(SavedItemExtract.class);
    }
}
