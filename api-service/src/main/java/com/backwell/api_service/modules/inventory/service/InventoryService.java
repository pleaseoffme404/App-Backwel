package com.backwell.api_service.modules.inventory.service;

import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.inventory.dto.ItemTransactionDTO;
import com.backwell.api_service.modules.inventory.entity.InventoryTrace;
import com.backwell.api_service.modules.inventory.repo.InventoryTraceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final UUIDService uuidService;
    private final InventoryTraceRepository inventoryTraceRepository;
    private final RedisInventoryCacheManager cacheManager;


    @Transactional
    public void registerProduct(List<ItemTransactionDTO> initStockTransactions) {
        // todo check no item id has a previous move
        List<InventoryTrace> traces = initStockTransactions.stream()
                .map(i-> InventoryTrace.builder()
                        .id(uuidService.next())
                        .item(i.item())

                        .physicalBalance(i.physicalDelta())
                        .physicalDelta(i.physicalDelta())

                        .availableBalance(i.availableDelta())
                        .availableDelta(i.availableDelta())

                        .redundancyBalance(i.redundancyDelta())
                        .redundancyDelta(i.redundancyDelta())

                        .reservedBalance(i.reservedDelta())
                        .reservedDelta(i.reservedDelta())

                        .build()
                ).toList();

        List<InventoryTrace> saved = inventoryTraceRepository.saveAllAndFlush(traces);

        // fuck you nigga :)
        saved.forEach(cacheManager::saveInitialInventory);
    }

    
    @Transactional
    public void saveItemInitialInventory(ItemTransactionDTO dto) {
        InventoryTrace trace = InventoryTrace.builder()
                .id(uuidService.next())
                .item(dto.item())

                .physicalBalance(dto.physicalDelta())
                .physicalDelta(dto.physicalDelta())

                .availableBalance(dto.availableDelta())
                .availableDelta(dto.availableDelta())

                .redundancyBalance(dto.redundancyDelta())
                .redundancyDelta(dto.redundancyDelta())

                .reservedBalance(dto.reservedDelta())
                .reservedDelta(dto.reservedDelta())
                .build();
        InventoryTrace savedTrace = inventoryTraceRepository.save(trace);
        cacheManager.saveInitialInventory(trace);
    }
}
