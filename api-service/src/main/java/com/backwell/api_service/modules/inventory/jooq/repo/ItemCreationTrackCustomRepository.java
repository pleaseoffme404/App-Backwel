package com.backwell.api_service.modules.inventory.jooq.repo;

import java.util.Set;
import java.util.UUID;

public interface ItemCreationTrackCustomRepository {


    boolean existsAtLeastOne(Set<UUID> itemIds);
    void saveItems(Set<UUID> itemIds);
}
