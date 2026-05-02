package com.backwell.api_service.modules.discount.repo;

import java.math.BigDecimal;
import java.util.UUID;

public interface DiscountCustomRepository {
    BigDecimal resolveDiscountForProduct(UUID productId);
    BigDecimal resolveDiscountForItem(UUID itemId);

}
