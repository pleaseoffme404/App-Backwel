package com.backwell.api_service.modules.products.jooq.impl;


import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.modules.products.dto.CategoryPath;
import com.backwell.api_service.modules.products.jooq.repo.CategoryCustomRepository;
import lombok.RequiredArgsConstructor;
import org.jooq.*;

import static com.backwell.api_service.jooq.generated.Tables.*;
import static org.jooq.impl.DSL.*;
import org.springframework.stereotype.Repository;

import java.util.List;
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
    public CategoryPath buildPath(UUID categoryId) {
        var C = CATEGORY;
        Name cteName = name("category_path");
        Field<UUID> idField = field(name("category_path", "id"), UUID.class);
        Field<String> nameField = field(name("category_path", "name"), String.class);
        Field<UUID> parentIdField = field(name("category_path", "parent_id"), UUID.class);

        // CTE Recursivo: Sube en el árbol desde la hoja hasta la raíz
        CommonTableExpression<?> cte = cteName.as(
                select(
                        C.ID.as("id"),
                        C.NAME.as("name"),
                        C.PARENT_ID.as("parent_id")
                ).from(C)
                        .where(C.ID.eq(categoryId))
                        .unionAll(
                                select(C.ID, C.NAME, C.PARENT_ID)
                                        .from(C)
                                        .join(cteName).on(C.ID.eq(parentIdField))
                        )
        );

        // Recupera la lista. Nota: Viene de la BD en orden inverso [Hoja -> ... -> Raíz]
        List<CategoryProjection> result = dsl.withRecursive(cte)
                .select(idField, nameField)
                .from(cte)
                .fetchInto(CategoryProjection.class);

        return CategoryProjection.toPaths(result.toArray(CategoryProjection[]::new));
    }

    /**
     * Proyección interna utilizada para mapear las filas del CTE.
     */
    private record CategoryProjection(UUID id, String name) {

        /**
         * Transforma las proyecciones obtenidas de la base de datos a la estructura final,
         * invirtiendo el orden para garantizar la jerarquía de [General -> Particular].
         */
        public static CategoryPath toPaths(CategoryProjection[] projections) {
            int len = projections.length;
            UUID[] ids = new UUID[len];
            String[] paths = new String[len];

            // PARCHE DE INVERSIÓN: Recorremos de adelante hacia atrás para rellenar de atrás hacia adelante
            for (int i = 0; i < len; i++) {
                int targetIndex = len - 1 - i; // Invierte el destino: i=0 va al final, i=último va al inicio (0)
                ids[targetIndex] = projections[i].id;
                paths[targetIndex] = projections[i].name;
            }

            return new CategoryPath(ids, paths);
        }
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
