package com.backwell.api_service.modules.products.jooq.repo;

import com.backwell.api_service.modules.products.jooq.dto.CartItemProjection;

import java.util.List;
import java.util.UUID;

public interface CartItemCustomRepository {
    List<CartItemProjection> getCartExtractForUser(UUID userId);
}
