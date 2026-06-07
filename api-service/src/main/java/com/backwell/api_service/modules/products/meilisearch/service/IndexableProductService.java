package com.backwell.api_service.modules.products.meilisearch.service;

import com.backwell.api_service.modules.discount.dto.CategoryDiscountExtract;
import com.backwell.api_service.modules.discount.dto.ProductDiscountExtract;
import com.backwell.api_service.modules.discount.service.DiscountService;
import com.backwell.api_service.modules.inventory.dto.RedisInventoryInfo;
import com.backwell.api_service.modules.inventory.service.RedisInventoryCacheManager;
import com.backwell.api_service.modules.products.controller.req.UpdateProductInfoRequest;
import com.backwell.api_service.modules.products.dto.CategoryPath;
import com.backwell.api_service.modules.products.jooq.dto.ItemAttributeTupleProjection;
import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import com.backwell.api_service.modules.products.jpa.entity.prod.Product;
import com.backwell.api_service.modules.products.jpa.repo.ItemAttributeRepository;
import com.backwell.api_service.modules.products.jpa.repo.ItemRepository;
import com.backwell.api_service.modules.products.jpa.repo.ProductRepository;
import com.backwell.api_service.modules.products.meilisearch.dto.IndexableProductDTO;
import com.backwell.api_service.modules.products.meilisearch.dto.StockLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexableProductService {

    private final ItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final DiscountService discountService;
    private final RedisInventoryCacheManager cacheManager;
    private final ItemAttributeRepository itemAttributeRepository;

    /**
     * Prepares the indexable document with all item information and expected pricing without a price check transaction id, so it won-'t be buyable until it has been processed by the pricing algorithm1*/
    public List<IndexableProductDTO> buildPostCreateIndexDocuments (Product product, CategoryPath categoryPath, CategoryDiscountExtract de) {
        Map<UUID, Map<String, String>> mappedAttributes = getMappedAttributes(product);


        return product.getItems().stream()
                .map(i-> {

                    BigDecimal currentPrice = de.hasDiscount()
                            ? de.decimalFactor().multiply(i.getBasePrice()).setScale(2, RoundingMode.HALF_UP)
                            : i.getBasePrice();

                    BigDecimal percentage = de.hasDiscount()
                            ? de.percentage().setScale(2, RoundingMode.HALF_UP)
                            : BigDecimal.ZERO;


                    RedisInventoryInfo inventoryInfo = cacheManager.getInventoryOrThrow(i.getId());

                    return IndexableProductDTO.builder()
                            .id(i.getId())
                            .productId(product.getId())
                            .visible(i.isVisible())
                            .name(product.getName())
                            .productDescription(product.getDescription())
                            .sku(i.getSku())
                            .brand(product.getBrand())
                            .mainPicture(i.getPictures().getFirst().getUrl())
                            .basePrice(i.getBasePrice())
                            .currentPrice(currentPrice)
                            .hasDiscount(de.hasDiscount())
                            .discountPercentage(percentage)
                            .inStock(inventoryInfo.hasAvailableStock())
                            .stockLevel(StockLevel.of(inventoryInfo).getLabel())

                            .categoryId(product.getCategory().getId())
                            .categoryHierarchy(Arrays.stream(categoryPath.namePath()).toList())
                            .attributes(mappedAttributes.get(i.getId()))
                            .lastUpdate(Instant.now())
                            .build();
                }).toList();
    }

    private Map<UUID, Map<String, String>> getMappedAttributes(Product product) {
        return product.getItems().stream()
                .collect(Collectors.toMap(
                        Item::getId,
                        i -> {
                            Map<String, String> itemAttributes = new HashMap<>();
                            i.getAttributes().forEach(a-> itemAttributes.put(
                                    a.getAttribute().getKey(),
                                    a.getValue()
                            ));
                            return itemAttributes;
                        }
                ));
    }

    public List<IndexableProductDTO> buildPostInfoUpdateDocuments(Product p, UpdateProductInfoRequest req) {
        List<UUID> itemIds = p.getItems().stream()
                .map(Item::getId)
                .toList();

        boolean attributesChanged = req.getAttributeKeyNames().isPresent();

        Map<UUID, Map<String, String>> mappedItemAttributes = attributesChanged ?
                itemAttributeRepository.mapItemAttributes(itemIds).entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().collect(Collectors.toMap(
                        ItemAttributeTupleProjection::attributeKey,
                        ItemAttributeTupleProjection::attributeValue
                ))
        )) : Map.of();



        if (attributesChanged) {
            Map<UUID, List<ItemAttributeTupleProjection>> mappedAttributes = itemAttributeRepository.mapItemAttributes(itemIds);

            mappedAttributes.forEach(
                    (itemId, attributes) -> mappedItemAttributes.put(
                            itemId,
                            attributes.stream().collect(Collectors.toMap(
                                    ItemAttributeTupleProjection::attributeKey,
                                    ItemAttributeTupleProjection::attributeValue
                            ))
                    )
            );
        }

        return itemIds.stream()
                .map(id -> {
                            var builder =  IndexableProductDTO.builder();
                            req.getName().ifPresent(builder::name);
                            req.getDescription().ifPresent(builder::productDescription);
                            req.getBrand().ifPresent(builder::brand);

                            Map<String, String> itemAttrs = mappedItemAttributes.get(id);
                            if (itemAttrs != null) {
                                builder.attributes(itemAttrs);
                            }

                            return builder.build();
                }).toList();
    }

    public IndexableProductDTO buildPostItemCreationDocument(Item i, Map<String, String> mappedAttributes, CategoryPath path, ProductDiscountExtract de) {
        Product p = i.getProduct();

        RedisInventoryInfo stockInfo = cacheManager.getInventoryOrThrow(i.getId());

        BigDecimal currentPrice = i.getBasePrice().multiply(de.percentage().setScale(2, RoundingMode.HALF_UP));


        return IndexableProductDTO.builder()
                .id(i.getId())
                .productId(i.getProductId())
                .visible(i.isVisible())
                .name(p.getName())
                .productDescription(p.getDescription())
                .sku(i.getSku())
                .brand(p.getBrand())
                .mainPicture(i.getPictures().getFirst().getUrl())
                .basePrice(i.getBasePrice())
                .currentPrice(currentPrice)
                .hasDiscount(de.hasDiscount())
                .discountPercentage(de.finalPrice(i.getBasePrice()))
                .inStock(stockInfo.hasAvailableStock())
                .stockLevel(stockInfo.stockLevelLabel())
                .categoryId(p.getCategoryId())
                .categoryHierarchy(Arrays.stream(path.namePath()).toList())
                .attributes(mappedAttributes)
                .lastUpdate(i.getUpdatedAt())
                .build();

    }
}
