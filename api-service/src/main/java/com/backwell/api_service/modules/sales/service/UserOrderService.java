package com.backwell.api_service.modules.sales.service;


import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.dto.MessageResponse;
import com.backwell.api_service.common.exception.BusinessException;
import com.backwell.api_service.common.util.UUIDService;
import com.backwell.api_service.modules.inventory.dto.ItemPricingDTO;
import com.backwell.api_service.modules.inventory.repo.PriceCalculationHistoryRepository;
import com.backwell.api_service.modules.products.jpa.entity.prod.Item;
import com.backwell.api_service.modules.products.jpa.repo.cart.CartItemRepository;
import com.backwell.api_service.modules.products.service.cart.ItemListsService;
import com.backwell.api_service.modules.sales.jpa.entity.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.backwell.api_service.common.exception.codes.OrderErrorCode.*;


@Service
@RequiredArgsConstructor
public class UserOrderService {
    private final CartItemRepository cartItemRepository;
    private final ItemListsService itemListsService;
    private final UUIDService uuidService;
    private final PriceCalculationHistoryRepository priceCalculationHistoryRepository;

    @Transactional
    public MessageResponse placeOrder(UserSession userSession, UUID idempotencyKey) {

        List<String> messages = itemListsService.auditAndRefreshCartAndSaved(userSession);

        // todo Correct this weak implementations
        if (messages.isEmpty()){
            String cartMessages = String.join(" , ", messages);
            String msg = "Current cartItems could not be purchased: [ " + cartMessages + " ]";
            throw new BusinessException(msg, CART_ITEMS_CONFLICT);
        }

        List<Item> cartItems = cartItemRepository.getCartItems(userSession.uuid());

        if (cartItems == null || cartItems.isEmpty()) {
            throw new BusinessException("Cart has no items", EMPTY_CART);
        }

//        Set<UUID> itemIds = cartItems.stream().map(Item::getId).collect(Collectors.toSet());
//        Map<UUID, ItemPricingDTO> pricesMap = priceCalculationHistoryRepository.getForItems(itemIds);
//
//        List<OrderItem> orderItems = cartItems.stream().map(item -> new OrderItem(
//                uuidService.next(),
//                item,
//                pricesMap.get(item.getId()).currentPrice(),
//                item
//
//        ))
        return null;
    }
}
