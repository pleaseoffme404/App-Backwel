package com.backwell.api_service.modules.products.jpa.service;

import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import com.backwell.api_service.modules.products.jpa.repo.ItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemService {
    private final ItemRepository itemRepository;

}
