package com.backwell.api_service.modules.inventory.jooq.impl;

import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.modules.inventory.jooq.repo.ItemCreationTrackCustomRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

import static com.backwell.api_service.jooq.generated.Tables.*;

@Repository
@RequiredArgsConstructor
public class ItemCreationTrackRepositoryImpl implements ItemCreationTrackCustomRepository {
    private final Supplier<OffsetDateTime> offsetDateTimeSupplier;
    private final DSLContext c;

    @Override
    public boolean existsAtLeastOne(Set<UUID> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return false;
        }

        var CT = ITEM_CREATION_TRACK;
        return c.fetchExists(
                c.selectOne()
                        .from(CT)
                        .where(CT.ITEM_ID.in(itemIds))
        );
    }

    @Override
    public void saveItems(Set<UUID> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return;
        }

        var CT = ITEM_CREATION_TRACK;
        boolean existsAny = c.fetchExists(
                c.selectOne()
                        .from(CT)
                        .where(CT.ITEM_ID.in(itemIds))
        );

        if (existsAny) {
            throw new SystemException("Can not create 2 creation registries for a same item");
        }

        OffsetDateTime now = offsetDateTimeSupplier.get();

        var insertIntoStep = c.insertInto(CT).columns(CT.ITEM_ID, CT.CREATED_AT);
        for (UUID itemId : itemIds) {
            insertIntoStep.values(itemId, now);
        }
        insertIntoStep.execute();
    }
}
