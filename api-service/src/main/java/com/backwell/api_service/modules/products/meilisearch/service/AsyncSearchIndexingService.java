package com.backwell.api_service.modules.products.meilisearch.service;

import com.backwell.api_service.modules.products.meilisearch.IndexName;
import com.backwell.api_service.modules.products.meilisearch.dto.IndexableProductDTO;
import com.backwell.api_service.modules.products.meilisearch.MeiliSearchIndexSettings;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.exceptions.MeilisearchException;
import com.meilisearch.sdk.model.Task;
import com.meilisearch.sdk.model.TaskInfo;
import com.meilisearch.sdk.model.TaskStatus;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncSearchIndexingService {
    private final Client client;
    private final ObjectMapper mapper;
    private final MeiliSearchIndexSettings settings;

    private final BlockingDeque<IndexableProductDTO> buffer = new LinkedBlockingDeque<>(10000);

    private ExecutorService consumerExecutor;
    private volatile boolean running = true;

    @PostConstruct
    protected void init() {
        this.consumerExecutor = Executors.newSingleThreadExecutor(r-> {
            Thread t = new Thread(r, "meilisearch-consumer-thread");
            t.setDaemon(true);
            return t;
        });

        this.consumerExecutor.submit(this::startBufferConsumer);
    }

    /**
     * Productores: Agregan elementos al buffer instantáneamente sin bloquear la lógica de negocio.
     */
    public void addToIndexBuffer(List<IndexableProductDTO> documents) {
        if (documents == null) return;

        for (IndexableProductDTO doc : documents) {
            addToIndexBuffer(doc);
        }
    }

    public void addToIndexBuffer(IndexableProductDTO itemModel) {
        boolean accepted = buffer.offer(itemModel);

        if (!accepted) {
            log.warn("[Meilisearch] Indexation Buffer id full");
        }
    }

    /**
     * Consumidor Asíncrono Continuo. Aquí reside el verdadero poder del Blocking Queue.
     */
    private void startBufferConsumer() {
        log.info("[Meilisearch] Starting meilisearch consumer thread");
        List<IndexableProductDTO> batch = new ArrayList<>();
        while (running || !buffer.isEmpty()) {
            try {
                IndexableProductDTO firstItem = buffer.poll(5, TimeUnit.SECONDS);
                if (firstItem != null) {
                    batch.add(firstItem);
                    buffer.drainTo(batch, settings.getBatchSize()-1);

                    log.info("Sending async batch of {} items to Meilisearch", batch.size());
                }
                addDocuments(batch);
                batch.clear();
            } catch (InterruptedException e) {
                log.info("[Meilisearch] Interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[Meilisearch] Unexpected error while consuming Meilisearch Index Buffer", e);
                batch.clear();
            }
        }
    }

    @PreDestroy
    protected void close() {
        log.info("Cerrando AsyncSearchIndexingService de forma ordenada (Graceful Shutdown)...");
        this.running = false;

        this.consumerExecutor.shutdown();
        try {
            // Esperamos un momento razonable para que termine de procesar lo que quede en la cola
            if (!this.consumerExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                log.warn("El consumidor de Meilisearch no terminó a tiempo, forzando apagado.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Retryable(
            retryFor = { MeilisearchException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000)
    )
    private void addDocuments(List<IndexableProductDTO> items) {
        if (items == null || items.isEmpty()) return;

        Index index = client.index(IndexName.PRODUCTS.name());
        try {
            log.info("[Meilsiearch] Indexing {} documents...", items.size());
            String json = mapper.writeValueAsString(items);

            TaskInfo taskInfo = index.addDocuments(json);

            client.waitForTask(taskInfo.getTaskUid());
            Task task = client.getTask(taskInfo.getTaskUid());

            if (task.getStatus() != TaskStatus.SUCCEEDED) {
                String errorDetails = task.getError() != null ? task.getError().getMessage() : "Unknown Error";
                log.error("MeiliSearch Indexation Task failed with status {}: {}",  task.getStatus(), errorDetails);
                throw new MeilisearchException("Task Failed in Meilisearch : " + errorDetails);
            }
        } catch (JsonProcessingException e) {
            log.error("Error al serializar productos para indexación", e);
        } catch (MeilisearchException e) {
            log.error("Error de comunicación con Meilisearch", e);
        }
    }
}