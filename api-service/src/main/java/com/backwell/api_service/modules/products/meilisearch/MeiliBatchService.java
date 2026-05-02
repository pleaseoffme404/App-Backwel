package com.backwell.api_service.modules.products.meilisearch;


import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

@Service
public class MeiliBatchService {
    private final BlockingDeque<IndexableProductDTO> buffer = new LinkedBlockingDeque<>();
    private final int BATCH_SIZE = 500;


    public void addToBuffer(IndexableProductDTO itemModel) {
        buffer.offer(itemModel);

        if (buffer.size() >= BATCH_SIZE) {
            flush();
        }
    }

    @Scheduled(fixedDelay = 300000)
    public synchronized void flush() {
        if (buffer.isEmpty()) return;

        List<IndexableProductDTO> batch = new ArrayList<>();
        buffer.drainTo(batch, BATCH_SIZE);
    }
}
