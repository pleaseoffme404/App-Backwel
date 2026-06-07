package com.backwell.api_service.modules.users.repo;

import com.backwell.api_service.modules.users.dto.CuponPagedResponse;
import com.backwell.api_service.modules.users.dto.CuponSearchFilters;

public interface CuponCustomRepository {
    CuponPagedResponse getCupons(CuponSearchFilters filters);
}
