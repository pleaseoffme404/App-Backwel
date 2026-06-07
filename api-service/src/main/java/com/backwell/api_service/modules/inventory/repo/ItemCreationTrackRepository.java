package com.backwell.api_service.modules.inventory.repo;

import com.backwell.api_service.modules.inventory.entity.ItemCreationTrack;
import com.backwell.api_service.modules.inventory.jooq.repo.ItemCreationTrackCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ItemCreationTrackRepository extends
        JpaRepository<ItemCreationTrack, UUID>,
        ItemCreationTrackCustomRepository
{

}
