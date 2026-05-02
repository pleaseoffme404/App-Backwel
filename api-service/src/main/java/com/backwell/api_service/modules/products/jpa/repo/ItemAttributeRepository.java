package com.backwell.api_service.modules.products.jpa.repo;


import com.backwell.api_service.modules.products.jpa.entity.prod.ItemAttribute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ItemAttributeRepository extends JpaRepository<ItemAttribute, UUID> {

}
