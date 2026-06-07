package com.backwell.api_service.modules.products.jooq.repo;

import com.backwell.api_service.modules.products.dto.SavedItemExtract;

import java.util.List;
import java.util.UUID;

public interface SavedLaterItemCustomRepository {

    List<SavedItemExtract> getSavedItemsExtract(UUID userUuid);
}
