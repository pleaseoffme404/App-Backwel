package com.backwell.api_service.modules.products.jpa.repo.cart;

import com.backwell.api_service.modules.products.dto.CartItemExtract;
import com.backwell.api_service.modules.products.jooq.repo.CartItemCustomRepository;
import com.backwell.api_service.modules.products.jpa.entity.cart.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, UUID>, CartItemCustomRepository {
}
