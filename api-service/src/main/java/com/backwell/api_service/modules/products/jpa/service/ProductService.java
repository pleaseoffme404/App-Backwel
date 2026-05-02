package com.backwell.api_service.modules.products.jpa.service;


import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.products.controller.dto.CreateProductRequest;
import com.backwell.api_service.modules.products.controller.dto.MessageResponse;
import com.backwell.api_service.modules.products.meilisearch.IndexableProductDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {
    private final UUIDService uuidService;
    private final SkuGenerator skuGenerator;


    @Transactional
    public MessageResponse create(CreateProductRequest req) {

        return null;
    }

    @Transactional(readOnly = true)
    public IndexableProductDTO buildIndexable(UUID itemId) {


        return null;

    }

}
