package com.backwell.api_service.modules.products.event;


import com.backwell.api_service.modules.products.meilisearch.dto.IndexableProductDTO;
import com.backwell.api_service.modules.products.meilisearch.service.AsyncSearchIndexingService;
import com.backwell.api_service.modules.products.meilisearch.service.IndexableProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
@Deprecated
public class ProductEventListener {
    private final IndexableProductService indexableProductService;
    private final AsyncSearchIndexingService indexingService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleUpdateCategoryPath(UpdateProductCategoryPathEvent event) {

        return;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCategoryRelocationEvent(CategoryRelocationEvent event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
