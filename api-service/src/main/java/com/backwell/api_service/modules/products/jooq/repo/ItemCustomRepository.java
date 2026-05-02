package com.backwell.api_service.modules.products.jooq.repo;

import com.backwell.api_service.common.util.CursorResponse;
import com.backwell.api_service.modules.products.controller.dto.CatalogItemDTO;

public interface ItemCustomRepository {
    CursorResponse<CatalogItemDTO> buildCatalog(CatalogItemDTO lastCursor);
}
