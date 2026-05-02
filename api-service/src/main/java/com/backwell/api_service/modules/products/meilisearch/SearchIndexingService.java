package com.backwell.api_service.modules.products.meilisearch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.model.TaskInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchIndexingService {
    private final Client client;
    private final ObjectMapper mapper;

    public void upsertProductsIndex(List<IndexableProductDTO> items) throws JsonProcessingException {
        Index index = client.index(IndexName.PRODUCTS.name());

        TaskInfo taskInfo = index.addDocuments(mapper.writeValueAsString(index));
        client.waitForTask(taskInfo.getTaskUid());
    }
}
