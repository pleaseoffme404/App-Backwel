package com.backwell.api_service.modules.products.jooq.impl;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.modules.inventory.dto.RedisInventoryInfo;
import com.backwell.api_service.modules.inventory.service.RedisInventoryCacheManager;
import com.backwell.api_service.modules.products.controller.res.CategoryStepDTO;
import com.backwell.api_service.modules.products.controller.res.ItemDTO;
import com.backwell.api_service.modules.products.controller.res.ProductDTO;
import com.backwell.api_service.modules.products.jpa.repo.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;

import static com.backwell.api_service.jooq.generated.Tables.*;
import static org.jooq.impl.DSL.*;

import static com.backwell.api_service.common.exception.codes.ProductErrorCode.*;

@Repository
@RequiredArgsConstructor
public class ProductDTORepository {
    private final DSLContext c;
    private final RedisInventoryCacheManager cacheManager;
    private final CategoryRepository categoryRepository;

    public ProductDTO fromDatabase(UUID productId) {
        var P = PRODUCT;
        var C = CATEGORY;
        var PA = PRODUCT_ATTRIBUTE;
        var I = ITEM;
        var IP = ITEM_PICTURE;
        var IA = ITEM_ATTRIBUTE;
        var PCH = PRICE_CALCULATION_HISTORY;

        // ping the database with the item id..., just in case.
        // It would be awful to return null after executing this massive query
        boolean exists = c.fetchExists(
                selectOne()
                .from(P)
                .where(P.ID.eq(productId))
        );

        if (!exists) {
            throw new BusinessException("Requested Product with id: `%s` was not found".formatted(productId), PRODUCT_NOT_FOUND);
        }



        // =================================================================
        // STEP 1: DECLARAR LAS CTEs COMO SI FUERAN TABLAS VIRTUALES
        // =================================================================
        Name rtName = name("ranked_transactions");
        CommonTableExpression<?> rankedTransactionsCte = rtName.as(
                select(
                        PCH.ITEM_ID.as("item_id"),
                        PCH.FINAL_PRICE.as("final_price"),
                        PCH.DISCOUNT_DECIMAL.as("discount_decimal"),
                        PCH.TRANSACTION_ID.as("transaction_id"),
                        rowNumber().over(partitionBy(PCH.ITEM_ID).orderBy(PCH.CREATED_AT.desc())).as("rn")
                ).from(PCH)
        );

        // Alias de campos para poder leer el resultado de la primera CTE
        Field<UUID> rtItemId = field(name("ranked_transactions", "item_id"), UUID.class);
        Field<BigDecimal> rtFinalPrice = field(name("ranked_transactions", "final_price"), BigDecimal.class);
        Field<BigDecimal> rtDiscountDecimal = field(name("ranked_transactions", "discount_decimal"), BigDecimal.class);
        Field<UUID> rtTransactionId = field(name("ranked_transactions", "transaction_id"), UUID.class);
        Field<Integer> rtRn = field(name("ranked_transactions", "rn"), Integer.class);

        Name ltName = name("latest_transactions");
        CommonTableExpression<?> latestTransactionsCte = ltName.as(
                select(rtItemId, rtFinalPrice, rtDiscountDecimal, rtTransactionId)
                        .from(rankedTransactionsCte)
                        .where(rtRn.eq(1))
        );

        // Campos definitivos expuestos por la CTE final para usar en nuestro JOIN
        Field<UUID> ltItemId = field(name("latest_transactions", "item_id"), UUID.class);
        Field<BigDecimal> ltFinalPrice = field(name("latest_transactions", "final_price"), BigDecimal.class);
        Field<BigDecimal> ltDiscountDecimal = field(name("latest_transactions", "discount_decimal"), BigDecimal.class);
        Field<UUID> ltTransactionId = field(name("latest_transactions", "transaction_id"), UUID.class);

        // =================================================================
        // STEP 2: EJECUTAR LA CONSULTA MAESTRA (WITH RECURSIVE / WITH)
        // =================================================================
        ProductMetadata m = c.with(rankedTransactionsCte, latestTransactionsCte) // Registramos las CTEs aquí
                .select(
                        P.ID.as("productId"),
                        P.CATEGORY_ID.as("categoryId"),
                        C.NAME.as("categoryName"),
                        P.BRAND.as("brand"),
                        P.NAME.as("productName"),
                        P.DESCRIPTION.as("description"),

                        // Multiset de Atributos del Producto
                        multiset(
                                select(PA.ID.as("attributeId"), PA.ATTRIBUTE_KEY.as("attributeKey"))
                                        .from(PA)
                                        .where(PA.PRODUCT_ID.eq(P.ID))
                        ).convertFrom(toProductAttribute).as("productAttributes"),

                        // Multiset principal de Ítems
                        multiset(
                                select(
                                        I.ID.as("itemId"),
                                        I.SKU.as("sku"),
                                        I.VISIBLE.as("visible"),
                                        multiset(
                                                select(IA.ID, IA.ATTRIBUTE_ID, IA.ATTRIBUTE_VALUE)
                                                        .from(IA)
                                                        .where(IA.ITEM_ID.eq(I.ID))
                                        ).convertFrom(toItemAttribute).as("itemAttributes"),

                                        multiset(
                                                select(IP.URL)
                                                        .from(IP)
                                                        .where(IP.ITEM_ID.eq(I.ID))
                                                        .orderBy(IP.IMAGE_ORDER.asc())
                                        ).convertFrom(toString).as("pictures"),
                                        I.BASE_PRICE,
                                        ltFinalPrice.as("finalPrice"),
                                        ltDiscountDecimal.multiply(BigDecimal.valueOf(100)).as("discountPercentage"),
                                        ltTransactionId.as("transactionId"),
                                        I.CREATED_AT.convertFrom(toInstant),
                                        I.UPDATED_AT.convertFrom(toInstant)
                                )
                                        .from(I)
                                        .leftJoin(latestTransactionsCte).on(I.ID.eq(ltItemId))
                        ).convertFrom(toItemProjection).as("items"),

                        P.CREATED_AT.convertFrom(toInstant).as("createdAt"),
                        P.UPDATED_AT.convertFrom(toInstant).as("lastUpdated")
                )
                .from(P)
                .join(C).on(P.CATEGORY_ID.eq(C.ID))
                .where(P.ID.eq(productId))
                .fetchOneInto(ProductMetadata.class);

        if (m == null) {
            throw new SystemException("Item with checked existence was not found. Imposible error");
        }

        Set<UUID> itemIds = new HashSet<>();
        m.items.forEach(item -> itemIds.add(item.itemId));

        Map<UUID, RedisInventoryInfo> stocks = cacheManager.getInventories(itemIds);

        CategoryStepDTO[] path = CategoryStepDTO.fromPath(categoryRepository.buildPath(m.categoryId));
        Map<UUID, String> productAttributes = new HashMap<>();
        m.productAttributes.forEach(a-> productAttributes.put(a.attributeId, a.attributeKey));

        List<ItemDTO> items = new ArrayList<>();
        m.items.forEach(i -> {
            RedisInventoryInfo info = stocks.get(i.itemId);

            Map<UUID, String> itemAttributes = new HashMap<>();
            i.itemAttributes.forEach(a-> itemAttributes.put(a.attributeKey, a.attributeValue));
            items.add(ItemDTO.builder()
                    .itemId(i.itemId)
                    .sku(i.sku)
                    .visible(i.visible)
                    .itemAttributes(itemAttributes)
                    .pictures(i.pictures)
                    .basePrice(i.basePrice)
                    .lastCheckedPrice(i.finalPrice)
                    .lastCheckedDiscountPercentage(i.discountPercentage)
                    .lastCheckTransaction(i.transactionId)
                    .availableStock(info.availableStock())
                    .reservedStock(info.reservedStock())
                    .redundancyStock(info.redundancyStock())
                    .physicalStock(info.physicalStock())
                    .createdAt(i.createdAt)
                    .lastUpdated(i.lastUpdated).build()
            );
        });

        return ProductDTO.builder()
                .productId(m.productId)
                .categoryId(m.categoryId)
                .categoryName(m.categoryName)
                .path(path)
                .brand(m.brand)
                .productName(m.productName)
                .description(m.description)
                .attributes(productAttributes)
                .items(items)
                .createdAt(m.createdAt)
                .lastUpdated(m.lastUpdated)
                .build();

    }

    private record ItemProjection(
            UUID itemId,
            String sku,
            Boolean visible,
            List<ItemAttributeProjection> itemAttributes,
            List<String> pictures,
            BigDecimal basePrice,
            BigDecimal finalPrice,
            BigDecimal discountPercentage,
            UUID transactionId,
            Instant createdAt,
            Instant lastUpdated
    ) {}

    private final Function<OffsetDateTime, Instant> toInstant = odt -> odt != null ? odt.toInstant() : null;

    private final Function<Result<Record2<UUID, String>>, List<ProductAttributeProjection>> toProductAttribute =
            r->r.into(ProductAttributeProjection.class);

    private final Function<Result<Record3<UUID, UUID, String>>, List<ItemAttributeProjection>> toItemAttribute =
            r->r.into(ItemAttributeProjection.class);

    private final Function<Result<Record1<String>>, List<String>> toString =
            r ->r.into(String.class);

    private final Function<Result<Record11<
            UUID,
            String,
            Boolean,
            List<ItemAttributeProjection>,
            List<String>,
            BigDecimal,
            BigDecimal,
            BigDecimal,
            UUID,
            Instant,
            Instant>>, List<ItemProjection>> toItemProjection= r-> r.into(ItemProjection.class);


    private record ProductAttributeProjection(
            UUID attributeId,
            String attributeKey
    ) {}

    private record ItemAttributeProjection(
            UUID attributeId,
            UUID attributeKey,
            String attributeValue
    ) {}

    private record ProductMetadata(
            UUID productId,
            UUID categoryId,
            String categoryName,
            String brand,
            String productName,
            String description,
            List<ProductAttributeProjection> productAttributes,
            List<ItemProjection> items,
            Instant createdAt,
            Instant lastUpdated
    ){}



}
