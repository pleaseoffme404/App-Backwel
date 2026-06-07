package com.backwell.api_service.modules.products.jooq.impl;

import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.modules.products.jooq.repo.ItemProductCategoryPathCustomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.jooq.impl.DSL.*;
import static com.backwell.api_service.jooq.generated.Tables.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ItemProductCategoryPathRepositoryImpl implements ItemProductCategoryPathCustomRepository {
    private final DSLContext c;


    @Override
    public void saveForProduct(Set<UUID> itemIds, UUID productId, UUID[] categoryPath) {
        if (itemIds == null || itemIds.isEmpty()) {
            throw new SystemException("Targeted Item Ids must not be null or empty");
        }

        var CP = ITEM_PRODUCT_CATEGORY_PATH;

        if (!checkAllNotExists(itemIds)) {
            throw new SystemException("Can not create two ItemCategoryPathregistries for a same item");
        }

        var insertStep = c.insertInto(CP)
                .columns(CP.ITEM_ID, CP.PRODUCT_ID, CP.CATEGORY_PATH);
        for (UUID itemId : itemIds) {
            insertStep.values(itemId, productId, categoryPath);
        }

        insertStep.execute();
    }

    @Override
    public void saveNewItemForProduct(UUID itemId, UUID productId, UUID[] categoryPath) {
        var CP = ITEM_PRODUCT_CATEGORY_PATH;

        c.insertInto(CP)
                .columns(CP.ITEM_ID, CP.PRODUCT_ID, CP.CATEGORY_PATH)
                .values(itemId, productId, categoryPath)
                .execute();
    }

    @Override
    public void updateProductPath(UUID productId, UUID[] newCategoryPath) {
        var IPCP = ITEM_PRODUCT_CATEGORY_PATH;

    }



    @Override
    public boolean checkAllNotExists(Set<UUID> itemIds) {
        var IPCP = ITEM_PRODUCT_CATEGORY_PATH;
        Integer count = c.select(count())
                .from(IPCP)
                .where(IPCP.ITEM_ID.in(itemIds))
                .fetchOne(0, int.class);
        Assert.notNull(count, "Impossible Error, this returns 0 at worst escenario");

        return count == 0;
    }
}
