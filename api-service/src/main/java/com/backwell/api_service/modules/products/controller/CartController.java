package com.backwell.api_service.modules.products.controller;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.modules.products.controller.req.cart.AddToCartRequest;
import com.backwell.api_service.modules.products.controller.req.cart.UpdateCartRequest;
import com.backwell.api_service.modules.products.controller.res.CartViewDTO;
import com.backwell.api_service.modules.products.service.cart.CartService;
import com.backwell.api_service.modules.products.service.cart.ItemListsService;
import com.backwell.api_service.modules.products.service.cart.SavedLaterListService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {
    private final CartService cartService;
    private final ItemListsService itemListsService;
    private final SavedLaterListService savedLaterListService;
    // Add items to cart


    @GetMapping("/")
    public ResponseEntity<CartViewDTO> getCart(
            UserSession userSession
    ) {
        var response = itemListsService.getCart(userSession);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/items")
    public ResponseEntity<Void> addToCart(
            UserSession userSession,
            @Valid @RequestBody AddToCartRequest requestBody
    ) {
        cartService.addToCart(userSession, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/items/amount")
    public ResponseEntity<CartViewDTO> updateCartItemQuantity(
            UserSession userSession,
            @Valid @RequestBody UpdateCartRequest req
    ) {
        var response = itemListsService.updateCartItem(userSession, req.itemId(), req.delta());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> removeFromCart(
            UserSession userSession,
            @RequestParam UUID itemId
    ) {
        boolean removed = cartService.removeFromCart(userSession, itemId);
        if (!removed) return ResponseEntity.noContent().build(); // 204 si no hubo cambios
        return ResponseEntity.ok().build();
    }

    @PostMapping("/saves/")
    public ResponseEntity<Void> addToSaved(
            UserSession userSession,
            @RequestParam UUID itemId
    ) {
        savedLaterListService.addItem(userSession, itemId);
        return ResponseEntity.ok().build();
    }


    @PostMapping("/items/save-for-later")
    public ResponseEntity<CartViewDTO> moveToSaved(
            UserSession userSession,
            @RequestParam UUID itemId
    ) {
        var response = itemListsService.saveForLater(userSession, itemId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/saves/move-to-cart")
    public ResponseEntity<CartViewDTO> moveToCart(
            UserSession userSession,
            @RequestParam UUID itemId
    ) {
        var response = itemListsService.saveForLater(userSession, itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/saves")
    public ResponseEntity<Void> removeFromSaved (
            UserSession session,
            @RequestParam UUID itemId
    ) {
        boolean removed = savedLaterListService.removeItem(session, itemId);
        if (!removed) return ResponseEntity.noContent().build();

        return ResponseEntity.ok().build();
    }
}
