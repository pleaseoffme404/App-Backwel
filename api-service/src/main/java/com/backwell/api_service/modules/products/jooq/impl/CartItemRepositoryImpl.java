package com.backwell.api_service.modules.products.jooq.impl;

import com.backwell.api_service.modules.products.jooq.dto.CartItemProjection;
import com.backwell.api_service.modules.products.jooq.repo.CartItemCustomRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.CommonTableExpression;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static com.backwell.api_service.jooq.generated.Tables.*;
import static org.jooq.impl.DSL.*;

@Repository
@RequiredArgsConstructor
public class CartItemRepositoryImpl implements CartItemCustomRepository {
    private final DSLContext c;


    @Override
    public List<CartItemProjection> getCartExtractForUser(UUID userId) {
        var ci = CART_ITEM;
        var i = ITEM;
        var p = PRODUCT;
        var ip = ITEM_PICTURE;
        var ph = PRICE_CALCULATION_HISTORY;
        
        CommonTableExpression<?> cartItems = name("cis").as(
                select(
                        ci.ITEM_ID.as("item_id"),
                        ci.SAVED_QUANTITY.as("saved_quantity")
                )
                        .from(ci)
                        .join(CART).on(ci.CART_ID.eq(CART.ID))
                        .where(CART.USER_ID.eq(userId))
        );
        
        Field<UUID> ciItemId = cartItems.field("item_id", UUID.class);
        Field<Integer> ciSavedQuantity = cartItems.field("saved_quantity", Integer.class);

        Assert.notNull(ciItemId, "item_id is null");
        Assert.notNull(ciSavedQuantity, "saved_quantity is null");
        
        return c
                .with(cartItems)
                .select(
                        ciItemId.as("itemId"),
                        i.SKU.as("sku"),
                        p.NAME.as("name"),
                        field(name("pic", "url"), String.class).as("pictureUrl"),
                        ciSavedQuantity.as("savedQuantity"),
                        i.LOGICAL_LIMIT.as("logicalLimit"),
                        field(name("lh", "final_price"), BigDecimal.class).as("unitPrice"),
                        field(name("lh", "discount_decimal"), BigDecimal.class).as("discountDecimal")
                )
                .from(cartItems)
                .join(ITEM).on(ciItemId.eq(i.ID))
                .join(PRODUCT).on(i.PRODUCT_ID.eq(p.ID))

                .crossJoin(
                        lateral(
                                select(ip.URL)
                                        .from(ITEM_PICTURE)
                                        .where(ip.ITEM_ID.eq(ciItemId))
                                        .orderBy(ip.IMAGE_ORDER)
                                        .limit(1)
                        ).as("pic")
                )

                .crossJoin(
                        lateral(
                                select(
                                        ph.BASE_PRICE,
                                        ph.FINAL_PRICE,
                                        ph.DISCOUNT_DECIMAL
                                )
                                        .from(PRICE_CALCULATION_HISTORY)
                                        .where(ph.ITEM_ID.eq(ciItemId))
                                        .orderBy(ph.CREATED_AT.desc())
                                        .limit(1)
                        ).as("lh")
                )
                .fetchInto(CartItemProjection.class);
    }
}
