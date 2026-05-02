package com.backwell.api_service.modules.products.jooq.impl;

import com.backwell.api_service.modules.products.jooq.dto.CartItemProjection;
import com.backwell.api_service.modules.products.jooq.repo.CartItemCustomRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import static com.backwell.api_service.jooq.generated.Tables.*;

@Repository
@RequiredArgsConstructor
public class CartItemRepositoryImpl implements CartItemCustomRepository {
    private final DSLContext dsl;


    @Override
    public List<CartItemProjection> getCartExtractForUser(UUID userId) {
        var CI = CART_ITEM;


        return List.of();
    }
}
