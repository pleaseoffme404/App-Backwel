package com.backwell.api_service.modules.products.jpa.repo;

import com.backwell.api_service.modules.products.controller.res.CategoryNodeDTO;
import com.backwell.api_service.modules.products.jooq.repo.CategoryCustomRepository;
import com.backwell.api_service.modules.products.jpa.entity.prod.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, CategoryCustomRepository {

    @NonNull
    Optional<Category> findById(@NonNull UUID id);


    @Query("""
SELECT new com.backwell.api_service.modules.products.controller.res.CategoryNodeDTO(
c.id,
c.name,
c.parent.id
)
FROM Category c""")
    List<CategoryNodeDTO> fetchAll();

    @Query("""
SELECT COUNT(c.id)
FROM Category c
WHERE c.id IN :ids""")
    long countExistingIds(@Param("ids") Set<UUID> ids);
}
