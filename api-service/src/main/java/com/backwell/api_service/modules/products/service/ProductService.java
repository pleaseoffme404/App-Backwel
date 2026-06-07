package com.backwell.api_service.modules.products.service;


import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.util.TransactionUtils;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.discount.dto.CategoryDiscountExtract;
import com.backwell.api_service.modules.discount.service.DiscountService;
import com.backwell.api_service.modules.inventory.dto.ItemTransactionDTO;
import com.backwell.api_service.modules.inventory.repo.ItemCreationTrackRepository;
import com.backwell.api_service.modules.inventory.service.InventoryService;
import com.backwell.api_service.modules.products.controller.req.CreateItemDTO;
import com.backwell.api_service.modules.products.controller.req.CreateProductRequest;
import com.backwell.api_service.modules.products.controller.req.UpdateProductInfoRequest;
import com.backwell.api_service.modules.products.controller.res.ProductDTO;
import com.backwell.api_service.modules.products.dto.CategoryPath;
import com.backwell.api_service.modules.products.jooq.repo.ItemProductCategoryPathCustomRepository;
import com.backwell.api_service.modules.products.jpa.entity.prod.*;
import com.backwell.api_service.modules.products.jpa.repo.CategoryRepository;
import com.backwell.api_service.modules.products.jpa.repo.ProductRepository;
import com.backwell.api_service.modules.products.meilisearch.dto.IndexableProductDTO;
import com.backwell.api_service.modules.products.meilisearch.service.AsyncSearchIndexingService;
import com.backwell.api_service.modules.products.meilisearch.service.IndexableProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.backwell.api_service.common.exception.codes.ProductErrorCode.*;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UUIDService uuidService;
    private final SkuGenerator skuGenerator;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final InventoryService inventoryService;
    private final ItemProductCategoryPathCustomRepository pathRepository;
    private final ItemCreationTrackRepository creationTrackRepository;
    private final DiscountService discountService;
    private final ProductDTOAssembler dtoAssembler;
    private final IndexableProductService indexableProductService;
    private final AsyncSearchIndexingService asyncSearchIndexingService;


    @Transactional
    public ProductDTO create(CreateProductRequest req) {
        Category category = categoryRepository.findById(req.categoryId())
                .orElseThrow(() -> new BusinessException(
                        String.format("Requested Category with Id: `%s`", req.categoryId()),
                        CATEGORY_NOT_FOUND
                ));

        Set<ProductAttribute> baseAttributes = buildAttributes(req);
        Product product = createProduct(req, category, baseAttributes);
        List<ItemTransactionDTO> createItemTransactions = processItemsBatch(req, product, baseAttributes);

        Product result = productRepository.saveAndFlush(product);

        Set<UUID> itemIds = result.getItems().stream().map(Item::getId).collect(Collectors.toSet());

        // saveItems creation move
        creationTrackRepository.saveItems(itemIds);

        CategoryPath path = categoryRepository.buildPath(req.categoryId());

        // for each item, create its register on the path
        pathRepository.saveForProduct(itemIds, result.getId(), path.idPath());

        // make the inventory transactions
        inventoryService.registerProduct(createItemTransactions);

        // calculate category discounts
        CategoryDiscountExtract discountExtract = discountService.getForCategoryPath(path);


        TransactionUtils.doAfterCommit(() -> {
            List<IndexableProductDTO> docs = indexableProductService.buildPostCreateIndexDocuments(result, path, discountExtract);
            asyncSearchIndexingService.addToIndexBuffer(docs);
        });

        return dtoAssembler.fromCreationInfo(result, path, discountExtract);
    }


    private Set<ProductAttribute> buildAttributes(CreateProductRequest req) {
        return req.attributes().stream()
                .map(a-> ProductAttribute.builder()
                        .id(uuidService.next())
                        .key(a)
                        .build()
                ).collect(Collectors.toSet());
    }

    private Product createProduct(CreateProductRequest req, Category category,  Set<ProductAttribute> baseAttributes) {
        Product product = Product.builder()
                .id(uuidService.next())
                .category(category)
                .brand(req.brand())
                .name(req.name())
                .description(req.description())
                .build();
        product.pushAttributes(baseAttributes);
        return product;
    }


    private List<ItemTransactionDTO> processItemsBatch(
            CreateProductRequest req,
            Product product,
            Set<ProductAttribute> baseAttributes
    ) {
        Set<CreateItemDTO> items = req.items();


        List<ItemTransactionDTO> transactions = new ArrayList<>();

        items.forEach(dto -> {
            // Crear entidad base
            Item itemEntity = createItem(dto, req.name());

            // Procesar Atributos
            setUpAttributes(itemEntity, baseAttributes, dto.itemAttributes());

            // Procesar Imágenes
            setUpPictures(itemEntity, dto.images());

            // Añadir la variante al producto
            product.addItem(itemEntity);

            int availableStock = dto.initialStock() - dto.redundancyStock();

            transactions.add(new ItemTransactionDTO(
                    itemEntity,
                    dto.initialStock(),
                    availableStock,
                    dto.redundancyStock(),
                    0
            ));
        });

        return transactions;
    }

    private Item createItem(CreateItemDTO dto, String productName) {
        String sku = skuGenerator.generateSku(productName, dto.itemAttributes());
        BigDecimal basePrice = dto.baseSalePrice().setScale(2, RoundingMode.HALF_UP);


        return Item.builder()
                .id(uuidService.next())
                .sku(sku)
                .basePrice(basePrice)
                .logicalLimit(dto.logicalLimit())
                .visible(true)
                .build();
    }

    private void setUpAttributes(
            Item item,
            Set<ProductAttribute> attributes,
            Map<String, String> itemAttributes
    ) {
        attributes.stream()
                .filter(attr-> itemAttributes.containsKey(attr.getKey()))
                .map(a -> ItemAttribute.builder()
                        .id(uuidService.next())
                        .attribute(a)
                        .value(itemAttributes.get(a.getKey()))
                        .build()
                ).forEach(item::addItemAttribute);
    }

    /**
     * Saves each item's images and assigns order value same as the image index in the images List
     * */
    private void setUpPictures(Item item, List<String> images) {
        for (int i = 0; i<images.size(); i++) {
            ItemPicture pic =  ItemPicture.builder()
                    .id(uuidService.next())
                    .url(images.get(i))
                    .order(i)
                    .build();
            item.addPicture(pic);
        }
    }

    @Transactional(readOnly = true)
    public ProductDTO getInfo(UUID productId) {
        return dtoAssembler.fromDatabase(productId);
    }

    @Transactional
    public ProductDTO updateProductInfo(UUID productId, UpdateProductInfoRequest req) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(
                        "Requested product with Id: `%s` was not found.".formatted(productId),
                        PRODUCT_NOT_FOUND
                ));

        req.getName().ifPresent(product::setName);
        req.getDescription().ifPresent(product::setDescription);
        req.getBrand().ifPresent(product::setBrand);
        req.getAttributeKeyNames().ifPresent(newAttributes -> {
            Map<UUID, ProductAttribute> attributeMap = product.getAttributes().stream()
                    .collect(Collectors.toMap(ProductAttribute::getId, Function.identity()));

            newAttributes.forEach((key, value) -> attributeMap.get(key).setKey(value));
        });

        Product saved = productRepository.saveAndFlush(product);

        // if no exception occurred...
        TransactionUtils.doAfterCommit(() -> {
            List<IndexableProductDTO> docs = indexableProductService.buildPostInfoUpdateDocuments(saved, req);
            asyncSearchIndexingService.addToIndexBuffer(docs);
        });

        return dtoAssembler.fromDatabase(productId);
    }

    @Transactional
    public ProductDTO updateCategory(UUID productId, UUID newCategoryId) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
