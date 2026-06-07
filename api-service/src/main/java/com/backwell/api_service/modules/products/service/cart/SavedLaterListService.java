package com.backwell.api_service.modules.products.service.cart;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.inventory.repo.PriceCalculationHistoryRepository;
import com.backwell.api_service.modules.inventory.service.RedisInventoryCacheManager;
import com.backwell.api_service.modules.products.controller.res.SavedItemDTO;
import com.backwell.api_service.modules.products.dto.SavedItemExtract;
import com.backwell.api_service.modules.products.jpa.entity.cart.SavedLaterItem;
import com.backwell.api_service.modules.products.jpa.entity.cart.SavedLaterList;
import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import com.backwell.api_service.modules.products.jpa.repo.ItemRepository;
import com.backwell.api_service.modules.products.jpa.repo.cart.SavedLaterItemRepository;
import com.backwell.api_service.modules.products.jpa.repo.cart.SavedLaterListRepository;

import com.backwell.api_service.modules.products.meilisearch.dto.StockLevel;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Validated
public class SavedLaterListService {
    private final SavedLaterListRepository savedLaterListRepository;
    private final SavedLaterItemRepository savedLaterItemRepository;
    private final RedisInventoryCacheManager inventoryCacheManager;
    private final UUIDService uuidService;
    private final ItemRepository itemRepository;

    @Transactional
    public void addItems(Iterable<SavedLaterItem> items, @NotNull UserSession session) {

        SavedLaterList savedLaterList = savedLaterListRepository.findListForUser(session.uuid())
                .orElseThrow(() -> new SystemException("No saved list found"));

        Set<UUID> containedItems = savedLaterList.getItems()
                .stream()
                .map(i -> i.getItem().getId())
                .collect(Collectors.toSet());


        for (SavedLaterItem item : items) {
            if (!containedItems.contains(item.getItemId())) {
                containedItems.add(item.getItemId());
                savedLaterList.addItem(item);
            }
        }

        savedLaterListRepository.save(savedLaterList);
    }

    @Transactional
    public void addItem(UserSession session, @NotNull UUID itemId) {
        Item i = itemRepository.getVisibleItemOrThrow(itemId);
        addItem(session, new SavedLaterItem(uuidService.next(),i));
    }

    @Transactional
    public void addItem(UserSession session, @NotNull SavedLaterItem item) {
        SavedLaterList savedLaterList = savedLaterListRepository.findListForUser(session.uuid())
                .orElseThrow(() -> new SystemException("No saved list found"));

        Set<UUID> containedItems= savedLaterList.getItems().stream()
                .map(SavedLaterItem::getItemId)
                .collect(Collectors.toSet());

        if (!containedItems.contains(item.getItemId())) {
            savedLaterList.addItem(item);
            savedLaterListRepository.save(savedLaterList);
        }
    }


    @Transactional
    public boolean removeItem(UserSession session, @NotNull UUID itemId) {

        SavedLaterList savedListGraph = savedLaterListRepository.findListForUser(Objects.requireNonNull(session.uuid()))
                .orElseThrow(() -> new SystemException("No saved list found"));

        return savedListGraph.getItems().stream()
                .filter(savedItem -> savedItem.getItem().getId().equals(itemId))
                .findFirst()
                .map(savedItem -> {
                    savedListGraph.removeItem(savedItem);
                    savedLaterListRepository.save(savedListGraph);
                    return true;
                })
                .orElse(false);
    }


    @Transactional
    public List<SavedItemDTO> getSavedItems (UserSession userSession) {
        List<SavedItemExtract> extracts = savedLaterItemRepository.getSavedItemsExtract(userSession.uuid());

        Set<UUID> itemIds = extracts.stream().map(SavedItemExtract::itemId).collect(Collectors.toSet());

        Map<UUID, StockLevel> stocks = inventoryCacheManager.getInventories(itemIds).entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e->StockLevel.of(e.getValue())
                ));


        return extracts.stream()
                .map(i->  SavedItemDTO.builder()
                        .itemId(i.itemId())
                        .name(i.name())
                        .mainPicture(i.mainPicture())
                        .sku(i.sku())
                        .basePrice(i.basePrice())
                        .currentPrice(i.currentPrice())
                        .discountDecimal(i.discountDecimal())
                        .stockLevel(stocks.getOrDefault(i.itemId(), StockLevel.NONE))
                        .build()
                )
                .toList();
    }
}
