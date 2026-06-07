package com.backwell.api_service.modules.products.service;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.common.util.TransactionUtils;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.discount.dto.ProductDiscountExtract;
import com.backwell.api_service.modules.discount.service.DiscountService;
import com.backwell.api_service.modules.inventory.dto.ItemTransactionDTO;
import com.backwell.api_service.modules.inventory.entity.ItemCreationTrack;
import com.backwell.api_service.modules.inventory.repo.ItemCreationTrackRepository;
import com.backwell.api_service.modules.inventory.service.InventoryService;
import com.backwell.api_service.modules.products.controller.req.CreateItemRequest;
import com.backwell.api_service.modules.products.controller.req.UpdateItemInfoRequest;
import com.backwell.api_service.modules.products.controller.res.ItemDTO;
import com.backwell.api_service.modules.products.dto.CategoryPath;
import com.backwell.api_service.modules.products.jooq.repo.ItemProductCategoryPathCustomRepository;
import com.backwell.api_service.modules.products.jpa.entity.prod.*;
import com.backwell.api_service.modules.products.jpa.repo.CategoryRepository;
import com.backwell.api_service.modules.products.jpa.repo.ItemRepository;
import com.backwell.api_service.modules.products.jpa.repo.ProductRepository;
import com.backwell.api_service.modules.products.meilisearch.dto.IndexableProductDTO;
import com.backwell.api_service.modules.products.meilisearch.service.AsyncSearchIndexingService;
import com.backwell.api_service.modules.products.meilisearch.service.IndexableProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.backwell.api_service.common.exception.codes.ProductErrorCode.*;
import static com.backwell.api_service.common.exception.codes.ItemErrorCode.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {
    private final ItemCreationTrackRepository creationTrackRepository;
    private final ItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final UUIDService uuidService;
    private final SkuGenerator skuGenerator;
    private final IndexableProductService indexableProductService;
    private final CategoryRepository categoryRepository;
    private final ItemProductCategoryPathCustomRepository pathRepository;
    private final InventoryService inventoryService;
    private final DiscountService discountService;
    private final AsyncSearchIndexingService asyncSearchIndexingService;

    @Transactional
    public ItemDTO createItem(CreateItemRequest req) {
        Product p = productRepository.findById(req.productId())
                .orElseThrow(() -> new BusinessException(
                        "Requested Product with Id: `%s` was not found.".formatted(req.productId()),
                        PRODUCT_NOT_FOUND
                ));

        Map<UUID, ProductAttribute> productAttributesMap = p.getAttributes().stream().collect(Collectors.toMap(
                ProductAttribute::getId,
                Function.identity()
        ));

        if (!productAttributesMap.keySet().equals(req.itemAttributes().keySet())) {
            throw new BusinessException(
                    "Provided item attribute map dos not match the product required attributes",
                    CREATION_ATTRIBUTES_MISMATCH
            );
        }

        List<ItemAttribute> itemAttributes = req.itemAttributes().entrySet()
                .stream()
                .map(entry -> ItemAttribute.builder()
                        .id(uuidService.next())
                        .attribute(productAttributesMap.get(entry.getKey()))
                        .value(entry.getValue())
                        .build()
                ).toList();

        Map<UUID, String> mappedAttributes = itemAttributes.stream()
                .collect(Collectors.toMap(
                        ItemAttribute::getKeyId,
                        ItemAttribute::getValue
                ));

        Map<String, String> attributeTuples = itemAttributes.stream().collect(Collectors.toMap(
                ItemAttribute::getKey,
                ItemAttribute::getValue
        ));

        String sku = skuGenerator.generateSku(p.getName(), attributeTuples);

        List<ItemPicture> pics = new ArrayList<>();
        for (int i = 0; i< req.images().size(); i++) {
            pics.add(ItemPicture.builder()
                    .id(uuidService.next())
                    .url(req.images().get(i))
                    .order(i)
                    .build());
        }

        Item newItem = Item.builder()
                .id(uuidService.next())
                .sku(sku)
                .basePrice(req.baseSalePrice().setScale(2, RoundingMode.HALF_UP))
                .logicalLimit(req.logicalLimit())
                .visible(true)
                .build();

        newItem.addItemAttributes(itemAttributes);
        newItem.addPictures(pics);

        p.addItem(newItem);

        productRepository.saveAndFlush(p);

        Item savedItem = itemRepository.findById(newItem.getId())
                .orElseThrow(() -> new SystemException("Race conditions failed. Imposibe Error"));


        // Get the creation move
        creationTrackRepository.save(new ItemCreationTrack(savedItem));

        // get the path and save it
        CategoryPath path = categoryRepository.buildPath(p.getCategoryId());
        pathRepository.saveNewItemForProduct(savedItem.getId(), p.getId(), path.idPath());


        // register inventory movements
        int availableStock = req.initialStock() - req.redundancyStock();
        ItemTransactionDTO initTransaction = new ItemTransactionDTO(
                savedItem,
                req.initialStock(),
                availableStock,
                req.redundancyStock(),
                0
        );

        inventoryService.saveItemInitialInventory(initTransaction);

        ProductDiscountExtract de = discountService.getDiscountForProduct(p.getId(), path);


        TransactionUtils.doAfterCommit(() -> {
            IndexableProductDTO document = indexableProductService.buildPostItemCreationDocument(savedItem, attributeTuples, path, de);
            asyncSearchIndexingService.addToIndexBuffer(document);
        });

        return buildFromInfo(savedItem, mappedAttributes, de,  initTransaction);
    }

    private ItemDTO buildFromInfo(Item i, Map<UUID, String> mappedAttributes, ProductDiscountExtract de, ItemTransactionDTO initTransaction) {
        List<String> pics = i.getPictures().stream()
                .map(ItemPicture::getUrl)
                .toList();

        BigDecimal finalPrice = de.decimalFactor().multiply(i.getBasePrice());


        return ItemDTO.builder()
                .itemId(i.getId())
                .sku(i.getSku())
                .visible(i.isVisible())
                .itemAttributes(mappedAttributes)
                .pictures(pics)
                .basePrice(i.getBasePrice())
                .lastCheckedPrice(finalPrice)
                .lastCheckedDiscountPercentage(de.percentage())
                .lastCheckTransaction(null)
                .availableStock(initTransaction.availableDelta())
                .reservedStock(initTransaction.reservedDelta())
                .redundancyStock(initTransaction.redundancyDelta())
                .physicalStock(initTransaction.physicalDelta())
                .createdAt(i.getCreatedAt())
                .lastUpdated(i.getUpdatedAt())
                .build();
    }

    @Transactional
    public ItemDTO updateItemInfo(UUID itemId, UpdateItemInfoRequest req) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
