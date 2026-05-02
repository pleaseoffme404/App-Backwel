package com.backwell.api_service.modules.products.meilisearch;

import com.backwell.api_service.modules.products.jpa.event.ItemSearchEvent;
import com.meilisearch.sdk.Client;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Component
@Slf4j
public class ItemSearchEventListener {
    private final Client client;

    @EventListener()
    public void onItemSearchEvent(ItemSearchEvent event) {




    }
}
