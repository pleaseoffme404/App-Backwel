package com.backwell.api_service.modules.products.event;

import com.backwell.api_service.modules.products.controller.req.UpdateProductInfoRequest;
import com.backwell.api_service.modules.products.jpa.entity.prod.Product;

@Deprecated
public record ProductInfoUpdateEvent(
        Product product,
        UpdateProductInfoRequest req
) {
}
