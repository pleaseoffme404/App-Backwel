package com.backwell.api_service.modules.products.jooq.impl;


import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.modules.products.jooq.repo.CategoryCustomRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.*;

import static com.backwell.api_service.jooq.generated.Tables.*;
import static org.jooq.impl.DSL.*;
import org.springframework.stereotype.Repository;

import java.util.UUID;

import static com.backwell.api_service.common.exception.codes.ProductErrorCode.*;

@RequiredArgsConstructor
@Repository
public class CategoryRepositoryImpl implements CategoryCustomRepository {
    private final DSLContext dsl;

    @Override
    public boolean hasProducts(UUID categoryId) {
        var P = PRODUCT;

        return dsl.fetchExists(
                dsl.selectOne()
                        .from(P)
                        .where(P.CATEGORY_ID.eq(categoryId))
        );
    }

    @Override
    public boolean hasChildren(UUID categoryId) {
        var C = CATEGORY;
        return dsl.fetchExists(
                dsl.selectOne()
                .from(C)
                        .where(C.PARENT_ID.eq(categoryId))
        );
    }

    @Override
    public String[] buildHierarchy(UUID targetId) {
        var C = CATEGORY;

        Name cteName = name("category_tree");
        Field<UUID> idField = field(name("category_tree", "id"), UUID.class);
        Field<String> nameField = field(name("category_tree", "name"), String.class);
        Field<UUID> parentIdField = field(name("category_tree", "parent_id"), UUID.class);

        CommonTableExpression<?> cte = cteName.as(
                select(
                        C.ID.as("id"),
                        C.NAME.as("name"),
                        C.PARENT_ID.as("parent_id")
                )
                        .from(C)
                        .where(C.ID.eq(targetId))
                        .unionAll(
                                select(C.ID, C.NAME, C.PARENT_ID)
                                        .from(C)
                                        .join(cteName).on(C.PARENT_ID.eq(parentIdField))
                        )
        );
        return dsl.withRecursive(cte)
                .select(nameField)
                .from(cte)
                .fetchArray(nameField);
    }

    @Override
    public boolean isDescending(UUID potentialParentId, UUID currentCategoryId) {
        if(potentialParentId.equals(currentCategoryId)) {
            return true;
        }

        var C = CATEGORY;

        Name cteName = name("check_cycle");
        Field<UUID> cteId = field(name("check_cycle", "id"), UUID.class);
        Field<UUID> parentField = field(name("check_cycle", "parent_id"), UUID.class);

        var cte = cteName.as(
                select(C.ID, C.PARENT_ID)
                .from(C)
                        .where(C.ID.eq(potentialParentId))
                        .unionAll(
                                select(C.ID, C.PARENT_ID)
                                        .from(C)
                                        .join(cteName).on(C.PARENT_ID.eq(parentField))
                        )
        );

        return dsl.withRecursive(cte)
                .selectOne()
                .from(cte)
                .where(cteId.eq(currentCategoryId))
                .fetchAny() != null;

    }

    @Override
    public void checkUniqueNameConstraint(String newName) {
        var C = CATEGORY;

        boolean existsWithName = dsl.fetchExists(
                dsl.selectOne().from(C).where(C.NAME.eq(newName)));
        if (existsWithName) {
            throw new BusinessException(
                    String.format("Category with name `%s` already exists`", newName),
                    CATEGORY_ALREADY_EXISTS.name()
            );
        }
    }
}
