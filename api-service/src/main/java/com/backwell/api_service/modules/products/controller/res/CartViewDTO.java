package com.backwell.api_service.modules.products.controller.res;

import java.math.BigDecimal;
import java.util.List;

public record CartViewDTO(
        int itemsCount,
        BigDecimal subtotal,
        List<String> updateMessages,
        List<CartItemDTO> items,
        List<SavedItemDTO> savedItems
) { }
