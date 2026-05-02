package com.backwell.api_service.modules.products.jpa.service;

import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.products.controller.dto.MessageResponse;
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
    public Category addCategory(String name, UUID parentId) throws IllegalStateException{
        if(name == null || name.isEmpty()){
            throw new IllegalArgumentException("El nombre de la categoria es obligatorio");
        }
        categoryRepository.checkUniqueNameConstraint(name);

        Category temp = categoryRepository.save(Category.builder()
                .id(uuidService.next())
                .name(name).build()
        );

        Category parent = fromUUID(parentId);
        parent.addChild(temp);
        categoryRepository.save(parent);
        return temp;
    }

    @Transactional
    public Category updateCategoryParentId(UUID targetId, UUID newParentId) {
        Category target = fromUUID(targetId);
        Category newParent = fromUUID(newParentId);

        if (!categoryRepository.isDescending(newParentId, targetId)) {
            String msg = String.format("Category `%s` could not update its parent to `%s`", target.getName(), newParent.getName());
            throw new IllegalArgumentException(msg);
        }

        newParent.addChild(target);
        categoryRepository.save(newParent);
        return target;
    }

    @Transactional
    public List<Category> insertCategoryGraph(List<CategoryNode> nodes) {
        categoryGraphValidator.validate(nodes);
        Map<String, Category> processed = new HashMap<>();
        List<Category> allInserted = new ArrayList<>();

        List<CategoryNode> currentLevel = nodes.stream().filter(n-> n.parentName()== null).toList();

        List<CategoryNode> remainig = new ArrayList<>(nodes);
        remainig.removeAll(currentLevel);

        while(!currentLevel.isEmpty()){
            List<Category> batch = currentLevel.stream().map(node -> {
                Category parent = node.parentName() != null ? processed.get(node.parentName()) : null;
                Category cat = Category.builder()
                        .id(uuidService.next())
                        .name(node.name())
                        .parent(parent)
                        .build();
                processed.put(node.name(), cat);
                return cat;
            }).toList();

            allInserted.addAll(batch);

            List<CategoryNode> nextLevel = new ArrayList<>();
            for(var it = remainig.iterator(); it.hasNext();){
                var node = it.next();
                if (processed.containsKey(node.name())){
                    nextLevel.add(node);
                    it.remove();
                }
            }
            currentLevel = nextLevel;
        }
        return categoryRepository.saveAll(allInserted);
    }


    @Transactional
    public MessageResponse renameCategory(String newName, UUID categoryId) {
        Category target = fromUUID(categoryId);

        categoryRepository.checkUniqueNameConstraint(newName);

        target.setName(newName);
        categoryRepository.save(target);
        String msg = String.format("Category with ID: `%s` was successfully renamed to `%s`", categoryId, newName);
        return new MessageResponse(msg);
    }

    @Transactional
    public MessageResponse attemptCategoryDelete(UUID categoryId) {
        Category target = fromUUID(categoryId);

        if (categoryRepository.hasProducts(categoryId) || categoryRepository.hasChildren(categoryId)) {
            String msg = String.format(
                    "Category with name: `%s` is not empty and can not be deleted.",
                    target.getName()
            );

            throw new BusinessException(msg, CATEGORY_DELETION_CONFLICT.name());
        }

        categoryRepository.delete(target);
        String template = String.format("Successfully deleted Category with ID: `%s`", categoryId);
        return new MessageResponse(template);
    }

    private Category fromUUID(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    String msg = String.format("Category with Id: `%s` was not found",categoryId);
                    return new BusinessException(msg, CATEGORY_NOT_FOUND.name());
                });
    }
}
