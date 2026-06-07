package com.backwell.api_service.modules.inventory.repo;

import com.backwell.api_service.modules.inventory.entity.ItemBasePriceChangeTrack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ItemBasePriceChangeTrackRepository extends JpaRepository<ItemBasePriceChangeTrack, Long> {

}
