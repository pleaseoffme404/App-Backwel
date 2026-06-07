package com.backwell.api_service.modules.products.service;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.products.controller.req.CreateCategoryGraphRequest;
import com.backwell.api_service.modules.products.controller.req.CreateCategoryRequest;
import com.backwell.api_service.modules.products.controller.req.RenameCategoryRequest;
import com.backwell.api_service.modules.products.controller.res.CategoryNodeDTO;
import com.backwell.api_service.modules.products.dto.CategoryNode;
import com.backwell.api_service.modules.products.jpa.entity.prod.Category;
import com.backwell.api_service.modules.products.jpa.repo.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import static com.backwell.api_service.common.exception.codes.ProductErrorCode.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final UUIDService uuidService;
    private final CategoryGraphValidator categoryGraphValidator;

    @Transactional
    public CategoryNodeDTO addCategory(CreateCategoryRequest req) {
        categoryRepository.checkUniqueNameConstraint(req.getCategoryName());

        Category newCategory = Category.builder()
                .id(uuidService.next())
                .name(req.getCategoryName())
                .build();

        req.getParentId().ifPresent(i-> {
            Category parent = categoryRepository.findById(i)
                    .orElseThrow(() -> new BusinessException(
                            "Parent Category with ID: `%s` was not found.".formatted(i),
                            CATEGORY_NOT_FOUND
                    ));
            parent.addChild(newCategory);
        });

        Category saved =  categoryRepository.save(newCategory);

        return CategoryNodeDTO.fromEntity(saved);
    }

    @Transactional
    public List<CategoryNodeDTO> createCategoryGraph(CreateCategoryGraphRequest req) {
        List<CategoryNode> nodes = req.getNodes();
        categoryGraphValidator.validate(nodes);

        Map<String, Category> createdMap = new HashMap<>();

        Category rootCategory = req.getParentId()
                .map(id -> categoryRepository.findById(id)
                        .orElseThrow(() -> new BusinessException(
                                "Parent Category with Id: `%s` was not found.".formatted(id),
                                CATEGORY_NOT_FOUND
                        ))
                ).orElse(null);

        List<CategoryNode> remaining = new ArrayList<>(nodes);

        while (!remaining.isEmpty()){
            int initialSize = remaining.size();

            var it = remaining.iterator();

            while(it.hasNext()){
                CategoryNode node = it.next();
                Category parentEntity = null;

                if (node.parentName() == null) {
                    parentEntity = rootCategory;
                } else if (createdMap.containsKey(node.parentName())) {
                    parentEntity = createdMap.get(node.parentName());
                }
                else {
                    continue;
                }

                Category newCategory = Category.builder()
                        .id(uuidService.next())
                        .name(node.name())
                        .parent(parentEntity)
                        .build();
                createdMap.put(node.name(), newCategory);
                it.remove();
            }

            if (remaining.size() == initialSize){
                throw new BusinessException(
                        "Cyclic Category Graph or orphan referencies detected", INVALID_CATEGORY_GRAPH
                );
            }
        }

        return categoryRepository.saveAll(createdMap.values()).stream()
                .map(CategoryNodeDTO::fromEntity)
                .toList();
    }


    // todo Produce event to reindex in meilisearch
    @Transactional
    public CategoryNodeDTO renameCategory(RenameCategoryRequest req) {
        Category target = fromUUID(req.targetId());
        categoryRepository.checkUniqueNameConstraint(req.newName());

        target.setName(req.newName());
        var saved = categoryRepository.save(target);

        return CategoryNodeDTO.fromEntity(saved);
    }

    // todo Produce event to reindex db and meilisearch
    @Transactional
    public CategoryNodeDTO updateCategoryParentId(UUID targetId, UUID newParentId) {
        Category target = fromUUID(targetId);
        Category newParent = fromUUID(newParentId);

        if (!categoryRepository.isDescending(newParentId, targetId)) {
            String msg = String.format("Category `%s` could not update its parent to `%s`", target.getName(), newParent.getName());
            throw new IllegalArgumentException(msg);
        }

        newParent.addChild(target);
        categoryRepository.save(newParent);
        return CategoryNodeDTO.fromEntity(target);
    }

    @Transactional
    public String attemptCategoryDelete(UUID categoryId) {
        Category target = fromUUID(categoryId);

        if (categoryRepository.hasProducts(categoryId) || categoryRepository.hasChildren(categoryId)) {
            String msg = String.format(
                    "Category with name: `%s` is not empty and can not be deleted.",
                    target.getName()
            );
            throw new BusinessException(msg, CATEGORY_DELETION_CONFLICT.name());
        }

        categoryRepository.delete(target);
        return String.format("Successfully deleted Category with ID: `%s`", categoryId);
    }

    @Transactional
    public List<CategoryNodeDTO> fetchAll() {
        return categoryRepository.fetchAll();
    }

    private Category fromUUID(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    String msg = String.format("Category with Id: `%s` was not found", categoryId);
                    return new BusinessException(msg, CATEGORY_NOT_FOUND.name());
                });
    }
}
