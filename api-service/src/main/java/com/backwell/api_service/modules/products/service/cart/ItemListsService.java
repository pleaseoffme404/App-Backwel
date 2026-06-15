package com.backwell.api_service.modules.products.service.cart;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.inventory.dto.RedisInventoryInfo;
import com.backwell.api_service.modules.inventory.service.RedisInventoryCacheManager;
import com.backwell.api_service.modules.products.controller.res.CartItemDTO;
import com.backwell.api_service.modules.products.controller.res.CartViewDTO;
import com.backwell.api_service.modules.products.controller.res.SavedItemDTO;
import com.backwell.api_service.modules.products.jpa.entity.cart.Cart;
import com.backwell.api_service.modules.products.jpa.entity.cart.CartItem;
import com.backwell.api_service.modules.products.jpa.entity.cart.SavedLaterItem;
import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import com.backwell.api_service.modules.products.jpa.repo.ItemRepository;
import com.backwell.api_service.modules.products.jpa.repo.cart.CartItemRepository;
import com.backwell.api_service.modules.products.jpa.repo.cart.CartRepository;
import com.backwell.api_service.modules.products.meilisearch.dto.StockLevel;
import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ItemListsService {
private final CartRepository cartRepository;
private final EntityManager em;
    private final SavedLaterListService savedLaterListService;
    private final RedisInventoryCacheManager  inventoryCacheManager;
    private final CartService cartService;
    private final UUIDService uuidService;
    private final ItemRepository itemRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public CartViewDTO getCart(UserSession userSession) {
        return getCartDTO(userSession);
    }

    private CartViewDTO getCartDTO(UserSession userSession) {
        List<String> messages = auditAndRefreshCartAndSaved(userSession);
        List<CartItemDTO> cartItems = cartService.getCart(userSession);
        List<SavedItemDTO> savedItems = savedLaterListService.getSavedItems(userSession);


        BigDecimal subtotal = cartItems.stream()
                .map(CartItemDTO::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Integer itemCount = cartItems.stream()
                .map(CartItemDTO::amount)
                .reduce(0, Integer::sum);

        return new CartViewDTO(itemCount, subtotal, messages, cartItems,  savedItems);
    }

    @Transactional
    public CartViewDTO updateCartItem(UserSession session, UUID itemId, int delta) {
        if (delta != 1 && delta != -1) {
            throw new BusinessException("Invald car update delta ammount", "fuckiu");
        }

        cartService.updateCartItem(session, itemId, delta);

        return getCartDTO(session);
    }

    @Transactional
    public CartViewDTO removeCartItem(UserSession session, UUID itemId) {
        cartService.removeFromCart(session, itemId);
        return getCartDTO(session);
    }

    @Transactional
    public void clearCart(@NotNull UUID cartId) {
        cartItemRepository.clearCart(cartId);
    }

    @Transactional
    @NonNull
    public List<String> auditAndRefreshCartAndSaved(UserSession userSession) {
        Cart root = cartRepository.findCartForUserId(userSession.uuid())
                .orElseThrow(() -> {
                    String msg = String.format("No cart was found for User with Id: `%s`.",  userSession.uuid());
                    return new SystemException(msg);
                });

        List<String> messages = new ArrayList<>();
        List<SavedLaterItem> toSaveItems = new ArrayList<>();

        List<CartItem> items = root.getCartItems();

        Map<UUID, RedisInventoryInfo> stocks = inventoryCacheManager.getInventories(
                items.stream()
                        .map(CartItem::getItemId)
                        .collect(Collectors.toSet())
        );

        Iterator<CartItem> it = items.iterator();

        while (it.hasNext()) {
            CartItem cItem = it.next();
            Item v = cItem.getItem();

            StockLevel level = StockLevel.of(stocks.get(v.getId()));

            if (level == StockLevel.NONE) {
                toSaveItems.add(new SavedLaterItem(uuidService.next(), v));
                String msg = String.format("Item `%s` was moved to saved later due to low stock.", v.getProduct().getName());
                messages.add(msg);

                it.remove();
                continue;
            }

            int availableStock = stocks.get(v.getId()).availableStock();
            if (cItem.getSavedQuantity() > availableStock) {
                cItem.setSavedQuantity(availableStock);
                messages.add("La cantidad de " + v.getProduct().getName() + " se ajustó al stock Disponible");
            }
        }

        if (!toSaveItems.isEmpty()) {
            savedLaterListService.addItems(toSaveItems, userSession);
        }

        cartRepository.save(root);
        return messages;
    }



    @Transactional
    public CartViewDTO saveForLater(UserSession session, UUID itemId) {
        Item i = itemRepository.getVisibleItemOrThrow(itemId);

        if (cartService.removeFromCart(session, itemId)) {
            savedLaterListService.addItem(session, new SavedLaterItem(uuidService.next(), i));
        }

        return getCartDTO(session);
    }

    @Transactional
    public CartViewDTO moveToCart(UserSession session, UUID itemId) {

        if (savedLaterListService.removeItem(session,  itemId)) {
            cartService.moveToCart(session, itemId);
        }
        return getCartDTO(session);
    }
}
