package com.backwell.api_service.modules.inventory.repo;


import com.backwell.api_service.modules.inventory.entity.InventoryTrace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InventoryTraceRepository extends JpaRepository<InventoryTrace, UUID> {
}
