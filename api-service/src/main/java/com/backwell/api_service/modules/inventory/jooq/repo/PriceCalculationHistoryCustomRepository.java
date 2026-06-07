package com.backwell.api_service.modules.inventory.jooq.repo;

import com.backwell.api_service.modules.inventory.dto.ItemPricingDTO;

import java.util.*;

public interface PriceCalculationHistoryCustomRepository {

    ItemPricingDTO getForItem(UUID itemId);

    Map<UUID, ItemPricingDTO> getForItems(Set<UUID> itemIds);


}
