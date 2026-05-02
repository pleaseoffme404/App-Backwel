package com.backwell.api_service.modules.products.jpa.service.cart;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.inventory.service.RedisStockService;
import com.backwell.api_service.modules.products.controller.dto.CartItemDTO;
import com.backwell.api_service.modules.products.dto.AddToCartRequest;
import com.backwell.api_service.modules.products.jooq.dto.CartItemProjection;
import com.backwell.api_service.modules.products.jpa.entity.cart.Cart;
import com.backwell.api_service.modules.products.jpa.entity.cart.CartItem;
import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import com.backwell.api_service.modules.products.jpa.repo.ItemRepository;
import com.backwell.api_service.modules.products.jpa.repo.cart.CartItemRepository;
import com.backwell.api_service.modules.products.jpa.repo.cart.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static com.backwell.api_service.common.exception.codes.ProductErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final RedisStockService stockService;
    private final UUIDService uuidService;

    private Cart extractCartForUser(UserSession session) {
        return cartRepository.findCartForUserId(session.uuid())
                .orElseThrow(() -> new SystemException(
                        String.format("No cart exists for User with Id: `%s`", session.uuid())
                ));
    }

    /** If available, adds an item to the cart with quantity 1*/
    @Transactional
    public void moveToCart(UserSession session, UUID itemId) {
        Item item = itemRepository.getVisibleItemOrThrow(itemId);
        int stock = stockService.getOrThrow(item.getId());
        if (stock == 0){
            throw new BusinessException(
                    String.format("Item with Id: `%s` is currently out of Stock",  item.getId()),
                    STOCK_CONFLICT.name()
            );
        }

        int stockLimit = Integer.min(stock, item.getLogicalLimit());
        Cart cart = extractCartForUser(session);
        cart.addOrUpdate(item, 1, stockLimit,uuidService);
        cartRepository.save(cart);
    }

    @Transactional
    public void addToCart(UserSession session, AddToCartRequest req) {
        Item item = itemRepository.getVisibleItemOrThrow(req.itemId());
        int stock = stockService.getOrThrow(item.getId());
        if (stock == 0){
            throw new BusinessException(
                    String.format("Item with Id: `%s` is currently out of Stock",  item.getId()),
                    STOCK_CONFLICT.name()
            );
        }

        int stockLimit = Integer.min(stock, item.getLogicalLimit());
        Cart cart = extractCartForUser(session);
        cart.addOrUpdate(item, req.amount(),  stockLimit,uuidService);
        cartRepository.save(cart);
    }

    @Transactional
    public boolean removeFromCart(UserSession session, UUID itemId) {
        Cart cart = extractCartForUser(session);
        List<CartItem> items = cart.getCartItems();
        return items.stream()
                .filter(i -> i.getItem().getId().equals(itemId))
                .findFirst()
                .map(item -> {
                    cart.removeCartItem(item);
                    cartRepository.save(cart);
                    return true;
                }).orElse(false);
    }


    @Transactional
    public void updateCartItem(UserSession session, UUID itemId, int delta) {
        Cart cart = extractCartForUser(session);
        Item item  = itemRepository.getVisibleItemOrThrow(itemId);
        int stock = stockService.getOrThrow(item.getId());
        int stockLimit = Integer.min(stock, item.getLogicalLimit());

        cart.updateExisting(item, delta, stockLimit);
        cartRepository.save(cart);
    }

    @Transactional
    // todo finish implementation
    public List<CartItemDTO> getCart(UserSession userSession) {
        List<CartItemProjection> extracts = cartItemRepository.getCartExtractForUser(userSession.uuid());

        return null;
    }






}
