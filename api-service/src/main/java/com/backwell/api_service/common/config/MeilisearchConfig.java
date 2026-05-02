package com.backwell.api_service.common.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MeilisearchConfig {

    @Bean
    public Client meilisearchClient(
            @Value("${app.meilisearch.host}") String host,
            @Value("${app.meilisearch.api-key}") String apiKey
    ) {
        return new Client(new Config(host, apiKey));
    }
}
