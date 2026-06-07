package com.backwell.api_service.modules.products.jooq.repo;

import com.backwell.api_service.modules.products.dto.CategoryPath;

import java.util.UUID;

public interface CategoryCustomRepository {
    boolean hasProducts(UUID categoryId);
    boolean hasChildren(UUID categoryId);
    boolean isDescending(UUID potentialParent, UUID currentCategoryId);

    /**
     * Construye la ruta jerárquica completa de una categoría utilizando una consulta recursiva (CTE).
     * * La ruta se genera en un orden top-down (desde lo general a lo particular), donde:
     * <ul>
     * <li>{@code index 0}: Corresponde a la categoría Raíz (Root / Nivel más general).</li>
     * <li>{@code index n}: Corresponde a la categoría consultada (Hoja / Nivel más específico).</li>
     * </ul>
     * * Este orden es crítico para optimizar las consultas de herencia por operadores de arreglos (@>)
     * e índices GIN en PostgreSQL, así como para facilitar la actualización parcial de sub-árboles.
     *
     * @param categoryId El identificador único de la categoría de la cual se desea trazar la ruta.
     * @return Un objeto {@link CategoryPath} con los arreglos de IDs y Nombres perfectamente ordenados.
     */
    CategoryPath buildPath(UUID categoryId);


    void checkUniqueNameConstraint(String newName);
}
