package com.backwell.api_service.modules.products.jpa.repo;

import com.backwell.api_service.modules.products.jooq.repo.CategoryCustomRepository;
import com.backwell.api_service.modules.products.jpa.entity.prod.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID>, CategoryCustomRepository {

    @NonNull
    Optional<Category> findById(@NonNull UUID id);
}
