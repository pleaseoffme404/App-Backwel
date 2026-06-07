package com.backwell.api_service.modules.products.service;

import com.backwell.api_service.modules.discount.dto.CategoryDiscountExtract;
import com.backwell.api_service.modules.inventory.dto.RedisInventoryInfo;
import com.backwell.api_service.modules.inventory.service.RedisInventoryCacheManager;
import com.backwell.api_service.modules.products.controller.res.CategoryStepDTO;
import com.backwell.api_service.modules.products.controller.res.ItemDTO;
import com.backwell.api_service.modules.products.controller.res.ProductDTO;
import com.backwell.api_service.modules.products.dto.CategoryPath;
import com.backwell.api_service.modules.products.jooq.impl.ProductDTORepository;
import com.backwell.api_service.modules.products.jpa.entity.prod.*;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Validated
public class ProductDTOAssembler {
    private final RedisInventoryCacheManager cacheManager;
    private final ProductDTORepository productDTORepository;

    public ProductDTO fromCreationInfo(Product p, CategoryPath path, CategoryDiscountExtract de) {

        Set<UUID> itemIds = new HashSet<>();
        p.getItems().forEach(item -> itemIds.add(item.getId()));

        Map<UUID, RedisInventoryInfo> redisInventoryInfoMap = cacheManager.getInventories(itemIds);

        var builder = ProductDTO.builder()
                .productId(p.getId())

                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())

                .path(CategoryStepDTO.fromPath(path))

                .brand(p.getBrand())

                .productName(p.getName())
                .description(p.getDescription())

                .attributes(
                        mapProductAttributes(p.getAttributes())
                )

                .createdAt(p.getCreatedAt())
                .lastUpdated(p.getUpdatedAt());

        List<ItemDTO> itemDTOs = p.getItems()
                .stream()
                .map(i-> mapCreationItemDTO(i,de, redisInventoryInfoMap))
                .toList();

        builder.items(itemDTOs);
        return builder.build();
    }

    private Map<UUID, String> mapProductAttributes(Set<ProductAttribute> productAttributes) {
        return productAttributes.stream().collect(Collectors.toMap(
                ProductAttribute::getId,
                ProductAttribute::getKey
        ));
    }

    private ItemDTO mapCreationItemDTO(Item i, CategoryDiscountExtract de, Map<UUID, RedisInventoryInfo> info) {
        var s = info.get(i.getId());

        BigDecimal lastPrice = i.getBasePrice()
                .multiply(de.decimalFactor())
                .setScale(2, RoundingMode.HALF_UP);



        return ItemDTO.builder()
                .itemId(i.getId())
                .sku(i.getSku())
                .visible(i.isVisible())
                .itemAttributes(
                        mapItemAttributes(i.getAttributes())
                )

                .pictures(
                        mapItemPictures(i.getPictures())
                )

                .basePrice(i.getBasePrice())
                .lastCheckedPrice(lastPrice)
                .lastCheckedDiscountPercentage(de.percentage())
                .lastCheckTransaction(null)
                .availableStock(s.availableStock())
                .reservedStock(s.reservedStock())
                .redundancyStock(s.redundancyStock())
                .physicalStock(s.physicalStock())
                .createdAt(i.getCreatedAt())
                .lastUpdated(i.getUpdatedAt())
                .build();
    }

    private Map<UUID, String> mapItemAttributes(Set<ItemAttribute> itemAttributes) {
        return itemAttributes.stream().collect(Collectors.toMap(
                ItemAttribute::getId,
                ItemAttribute::getValue
        ));
    }

    private List<String> mapItemPictures(List<ItemPicture> pics) {
        return pics.stream().map(ItemPicture::getUrl).collect(Collectors.toList());
    }

    // the fucking method
    public ProductDTO fromDatabase(@NotNull UUID productId) {
        return productDTORepository.fromDatabase(productId);
    }
}
