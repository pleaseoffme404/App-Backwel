package com.backwell.api_service.modules.products.service.cart;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.inventory.dto.RedisInventoryInfo;
import com.backwell.api_service.modules.inventory.service.RedisInventoryCacheManager;
import com.backwell.api_service.modules.products.controller.res.CartItemDTO;
import com.backwell.api_service.modules.products.controller.req.cart.AddToCartRequest;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.backwell.api_service.common.exception.codes.ProductErrorCode.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ItemRepository itemRepository;
    private final RedisInventoryCacheManager cacheManager;
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

        int stock = cacheManager.getAvailableOrThrow(itemId);


        if (stock == 0){
            throw new BusinessException(
                    String.format("Item with Id: `%s` is currently out of Stock",  item.getId()),
                    STOCK_CONFLICT
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
        int stock = cacheManager.getAvailableOrThrow(req.itemId());
        if (stock == 0){
            throw new BusinessException(
                    String.format("Item with Id: `%s` is currently out of Stock",  item.getId()),
                    STOCK_CONFLICT
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
        int stock = cacheManager.getAvailableOrThrow(itemId);
        int stockLimit = Integer.min(stock, item.getLogicalLimit());

        cart.updateExisting(item, delta, stockLimit);
        cartRepository.save(cart);
    }

    @Transactional
    public List<CartItemDTO> getCart(UserSession userSession) {
        List<CartItemProjection> extracts = cartItemRepository.getCartExtractForUser(userSession.uuid());

        Set<UUID> itemIds = extracts.stream().map(CartItemProjection::itemId).collect(Collectors.toSet());

        Map<UUID, RedisInventoryInfo> stocks = cacheManager.getInventories(itemIds);

        return extracts.stream().map(e-> {
            int stockLimit = Integer.min(
                    e.logicalLimit(),
                    stocks.get(e.itemId()).availableStock()
            );
            BigDecimal lineTotal = e.unitPrice()
                    .multiply(BigDecimal.valueOf(e.savedQuantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            return CartItemDTO.builder()
                    .itemId(e.itemId())
                    .sku(e.sku())
                    .name(e.name())
                    .pictureUrl(e.pictureUrl())
                    .amount(e.savedQuantity())
                    .stockLimit(stockLimit)
                    .unitPrice(e.unitPrice())
                    .lineTotal(lineTotal)
                    .discountDecimal(e.discountDecimal())
                    .build();
        }).toList();
    }






}
