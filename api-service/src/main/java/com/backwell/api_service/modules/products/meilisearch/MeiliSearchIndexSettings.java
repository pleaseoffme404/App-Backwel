package com.backwell.api_service.modules.products.meilisearch;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

@Configuration
@ConfigurationProperties("app.meilisearch.index")
@Validated
@Data
public class MeiliSearchIndexSettings {
    private final int batchSize = 500;
    private long pollIntervalMillis = 300000;

}
