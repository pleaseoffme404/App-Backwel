package com.backwell.api_service.modules.sales.controller;

import com.backwell.api_service.common.config.user.UserSession;
import com.backwell.api_service.common.idempotency.GlobalIdempotencyCache;
import com.backwell.api_service.modules.sales.service.UserOrderService;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

import static com.backwell.api_service.common.idempotency.GlobalIdempotencyCache.STATE_COMPLETED;
import static com.backwell.api_service.common.idempotency.GlobalIdempotencyCache.STATE_IN_PROGRESS;
import static com.backwell.api_service.common.idempotency.IdempotencyDomain.SALES;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserOrdersController {
    private final UserOrderService userOrderService;
    private final GlobalIdempotencyCache idempotencyCache;

    @PostMapping("/order/user")
    ResponseEntity<?> placeUserOrder(
            UserSession userSession,
            @NotNull @RequestParam UUID idempotencyKey
    ) {
        boolean isNew = idempotencyCache.startRequest(
                SALES,
                userSession.uuid(),
                idempotencyKey,
                Duration.ofMinutes(5)
        );

        if (!isNew) {
            String currentStatus = idempotencyCache.getRequestStatus(SALES, userSession.uuid(), idempotencyKey);
            if (STATE_IN_PROGRESS.equals(currentStatus)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body("Processing request...");
            }

            if (STATE_COMPLETED.equals(currentStatus)) {
                return ResponseEntity.noContent().build();
            }
        }

        var response = userOrderService.placeOrder(userSession, idempotencyKey);
        idempotencyCache.completeRequest(SALES, userSession.uuid(), idempotencyKey, Duration.ofDays(1));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/")
    ResponseEntity<?> getOrders(UserSession session) {

        //todo obtener los pedidos por cada uno de los usuarios
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
