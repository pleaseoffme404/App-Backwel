package com.backwell.api_service.modules.discount.jooq.repo;

import com.backwell.api_service.modules.discount.dto.CategoryDiscountExtract;
import com.backwell.api_service.modules.discount.dto.ProductDiscountExtract;
import com.backwell.api_service.modules.discount.controller.req.DiscountTargetsDTO;
import com.backwell.api_service.modules.discount.controller.res.DiscountDTO;
import com.backwell.api_service.modules.discount.controller.res.DiscountExtractDTO;

import java.util.List;
import java.util.UUID;

public interface DiscountCustomRepository {
    CategoryDiscountExtract resolveDiscountForCategory(UUID[] categoryPath);

    ProductDiscountExtract resolveDiscountForProduct(UUID[] categoryPath, UUID productId);

    DiscountExtractDTO getDiscountDetails(UUID discountId);

    void popDiscountTargets(UUID discountId, DiscountTargetsDTO dto);

    void addDiscountTargets(UUID discountId, DiscountTargetsDTO dto);
}
