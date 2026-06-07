package com.backwell.api_service.modules.users.dto;

import java.util.List;

public record CuponPagedResponse(
        boolean hasNext,
        List<CuponDTO> payload
) {
}
