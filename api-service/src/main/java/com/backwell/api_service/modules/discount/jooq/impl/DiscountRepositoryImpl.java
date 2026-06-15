package com.backwell.api_service.modules.discount.jooq.impl;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.modules.discount.dto.CategoryDiscountExtract;
import com.backwell.api_service.modules.discount.dto.ProductDiscountExtract;
import com.backwell.api_service.modules.discount.controller.req.DiscountTargetsDTO;
import com.backwell.api_service.modules.discount.controller.res.DiscountExtractDTO;
import com.backwell.api_service.modules.discount.jooq.dto.DiscountMetadata;
import com.backwell.api_service.modules.discount.jooq.repo.DiscountCustomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Supplier;

import static com.backwell.api_service.jooq.generated.Tables.*;
import static com.backwell.api_service.common.exception.codes.DiscountErrorCode.*;


@Repository
@RequiredArgsConstructor
@Slf4j
public class DiscountRepositoryImpl implements DiscountCustomRepository {
    private final DSLContext dsl;
    private final Supplier<OffsetDateTime> utcNowSupplier;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public CategoryDiscountExtract resolveDiscountForCategory(UUID[] categoryPath) {
        String sql = "SELECT fn_process_category_discount(:categoryPath)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("categoryPath", categoryPath);

        BigDecimal discountValue = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        return CategoryDiscountExtract.of(discountValue);
    }

    @Override
    public ProductDiscountExtract resolveDiscountForProduct(UUID[] categoryPath, UUID productId) {
        String sql = "SELECT fn_process_product_discount(:categoryPath, :productUuid)";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("categoryPath", categoryPath)
                .addValue("productUuid", productId);

        BigDecimal discountValue = jdbcTemplate.queryForObject(sql, params, BigDecimal.class);
        return ProductDiscountExtract.of(discountValue);
    }

    @Override
    public DiscountExtractDTO getDiscountDetails(UUID discountId) {
        var d = DISCOUNT;
        var dt = DISCOUNT_TARGET;
        var i = ITEM;
        var p = PRODUCT;
        var c = CATEGORY;

        DiscountMetadata metadata = dsl.select(
                        d.ID,
                        d.NAME,
                        d.DECIMAL_VALUE,
                        d.STACKABLE,
                        d.ACTIVE,
                        d.START_DATE,
                        d.END_DATE,
                        d.CREATED_AT,
                        d.LAST_UPDATE
                )
                .from(d)
                .where(d.ID.eq(discountId))
                .fetchOne(r -> new DiscountMetadata(
                        r.get(d.ID),
                        r.get(d.NAME),
                        r.get(d.DECIMAL_VALUE),
                        r.get(d.STACKABLE),
                        r.get(d.ACTIVE),
                        r.get(d.START_DATE).toInstant(),
                        r.get(d.END_DATE).toInstant(),
                        r.get(r.field(d.CREATED_AT)).toInstant(),
                        r.get(r.field(d.LAST_UPDATE)).toInstant()
                ));

        if (metadata == null) {
            throw new BusinessException(String.format("Discount with Id: '%s' was not found.", discountId), DISCOUNT_NOT_FOUND);
        }

        Map<UUID, String> itemsRelated = new HashMap<>();
        Map<UUID, String> productsRelated = new HashMap<>();
        Map<UUID, String> categoriesRelated = new HashMap<>();

        dsl.select(dt.ITEM_ID, i.SKU, dt.PRODUCT_ID, p.NAME, dt.CATEGORY_ID, c.NAME)
                .from(dt)
                .leftJoin(i).on(dt.ITEM_ID.eq(i.ID))
                .leftJoin(p).on(dt.PRODUCT_ID.eq(p.ID))
                .leftJoin(c).on(dt.CATEGORY_ID.eq(c.ID))
                .where(dt.DISCOUNT_ID.eq(discountId))
                .fetch()
                .forEach(r -> {
                    if (r.get(dt.ITEM_ID) != null) itemsRelated.put(r.get(dt.ITEM_ID), r.get(i.SKU));
                    if (r.get(dt.PRODUCT_ID) != null) productsRelated.put(r.get(dt.PRODUCT_ID), r.get(p.NAME));
                    if (r.get(dt.CATEGORY_ID) != null) categoriesRelated.put(r.get(dt.CATEGORY_ID), r.get(c.NAME));
                });

        return DiscountExtractDTO.from(metadata, categoriesRelated, productsRelated, itemsRelated);
    }

    @Override
    public void popDiscountTargets(UUID discountId, DiscountTargetsDTO dto) {
        if (!isUpdatable(discountId)) {
            throw new BusinessException(
                    "Discount with Id: `%s` was not found or is not updatable.".formatted(discountId),
                    NOT_UPDATABLE_DISCOUNT
            );
        }

        var dt = DISCOUNT_TARGET;

        // 1. Delete Items safely scoped to this specific discount
        if (dto.itemTargets() != null && !dto.itemTargets().isEmpty()) {
            dsl.deleteFrom(dt)
                    .where(dt.DISCOUNT_ID.eq(discountId))
                    .and(dt.ITEM_ID.in(dto.itemTargets()))
                    .execute();
        }

        // 2. Delete Products safely scoped to this specific discount
        if (dto.productTargets() != null && !dto.productTargets().isEmpty()) {
            dsl.deleteFrom(dt)
                    .where(dt.DISCOUNT_ID.eq(discountId))
                    .and(dt.PRODUCT_ID.in(dto.productTargets()))
                    .execute();
        }

        // 3. Delete Categories safely scoped to this specific discount
        if (dto.categoryTargets() != null && !dto.categoryTargets().isEmpty()) {
            dsl.deleteFrom(dt)
                    .where(dt.DISCOUNT_ID.eq(discountId))
                    .and(dt.CATEGORY_ID.in(dto.categoryTargets()))
                    .execute();
        }
    }

    @Override
    public void addDiscountTargets(UUID discountId, DiscountTargetsDTO dto) {
        if (!isUpdatable(discountId)) {
            throw new BusinessException(
                    "Discount with Id: `%s` was not found or is not updatable.".formatted(discountId),
                    NOT_UPDATABLE_DISCOUNT
            );
        }
        var dt = DISCOUNT_TARGET;

        // Example implementation using jOOQ's batch insert capabilities
        var insert = dsl.insertInto(dt, dt.DISCOUNT_ID, dt.ITEM_ID, dt.PRODUCT_ID, dt.CATEGORY_ID);
        boolean hasRows = false;

        if (dto.itemTargets() != null) {
            for (UUID itemId : dto.itemTargets()) {
                insert.values(discountId, itemId, null, null);
                hasRows = true;
            }
        }
        if (dto.productTargets() != null) {
            for (UUID productId : dto.productTargets()) {
                insert.values(discountId, null, productId, null);
                hasRows = true;
            }
        }
        if (dto.categoryTargets() != null) {
            for (UUID catId : dto.categoryTargets()) {
                insert.values(discountId, null, null, catId);
                hasRows = true;
            }
        }

        if (hasRows) {
            insert.execute();
        }
    }

    private boolean isUpdatable(UUID discountId) {
        var d = DISCOUNT;
        return dsl.fetchExists(dsl.selectOne()
                .from(d)
                .where(d.ID.eq(discountId))
                .and(d.END_DATE.gt(utcNowSupplier.get())));
    }
}