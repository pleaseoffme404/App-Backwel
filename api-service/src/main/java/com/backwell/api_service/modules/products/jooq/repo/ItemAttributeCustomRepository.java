package com.backwell.api_service.modules.products.jooq.repo;

import com.backwell.api_service.modules.products.jooq.dto.ItemAttributeTupleProjection;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ItemAttributeCustomRepository {

    Map<UUID, List<ItemAttributeTupleProjection>> mapItemAttributes(Collection<UUID> itemIds);
}
