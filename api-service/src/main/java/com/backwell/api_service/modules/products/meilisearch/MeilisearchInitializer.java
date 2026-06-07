package com.backwell.api_service.modules.products.meilisearch;

import com.backwell.api_service.modules.products.meilisearch.dto.IndexableProductDTO;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.Settings;
import com.meilisearch.sdk.model.TaskInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MeilisearchInitializer implements CommandLineRunner {
    private final Client client;

    @Override
    public void run(String... args) throws Exception {
        try {
            log.info("Starting Meilisearch initializer ...");
            Index index = client.index(IndexName.PRODUCTS.name());

            // Settings for this index
            Settings settings = new Settings();
            settings.setDistinctAttribute(IndexableProductDTO.Fields.productId);
            settings.setSearchableAttributes(new String[]{
                    IndexableProductDTO.Fields.name,
                    IndexableProductDTO.Fields.categoryHierarchy,
                    IndexableProductDTO.Fields.brand,
                    IndexableProductDTO.Fields.attributes
            });

            settings.setFilterableAttributes(new String[]{
                    IndexableProductDTO.Fields.brand,
                    IndexableProductDTO.Fields.categoryId,
                    IndexableProductDTO.Fields.inStock,
                    IndexableProductDTO.Fields.hasDiscount,
                    IndexableProductDTO.Fields.basePrice,
                    IndexableProductDTO.Fields.currentPrice
            });

            settings.setSortableAttributes(new String[] {
                    IndexableProductDTO.Fields.name,
                    IndexableProductDTO.Fields.currentPrice,
                    IndexableProductDTO.Fields.lastUpdate
            });

            TaskInfo task = index.updateSettings(settings);
            client.waitForTask(task.getTaskUid());


            log.info("Meilisearch configurado correctamente.");

        } catch (Exception e) {
            log.error("Error inicializando Meilisearch: {}", e.getMessage());
        }
    }
}