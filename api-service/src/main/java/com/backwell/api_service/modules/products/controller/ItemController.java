package com.backwell.api_service.modules.products.controller;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.modules.products.controller.req.CreateItemDTO;
import com.backwell.api_service.modules.products.controller.req.CreateItemRequest;
import com.backwell.api_service.modules.products.controller.req.UpdateItemInfoRequest;
import com.backwell.api_service.modules.products.controller.res.ItemDTO;
import com.backwell.api_service.modules.products.service.ItemService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@Validated
public class ItemController {
    private final ItemService itemService;

    @PostMapping("/")
    public ResponseEntity<ItemDTO> addItem(
            @Valid @NotNull @RequestBody CreateItemRequest req
    ) {
        var response = itemService.createItem(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // todo finish implementation
    @PatchMapping("/")
    public ResponseEntity<ItemDTO> updateItemInfo(
            @NotNull @RequestParam UUID itemId,
            @NotNull @Valid @RequestBody UpdateItemInfoRequest req) {
        var response = itemService.updateItemInfo(itemId, req);
        return ResponseEntity.ok(response);
    }

}
