package com.disthropic.api_gateway.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {
    @Value("${fallback.messages.user-service:User service unavailable}")
    private String userServiceFallback;

    @Value("${fallback.messages.order-service:Order service unavailable}")
    private String orderServiceFallback;

    @Value("${fallback.messages.default:Service temporarily unavailable}")
    private String defaultFallback;

    @GetMapping("/users")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        log.warn("User service fallback triggered");
        return createFallbackResponse("user-service", userServiceFallback);
    }

    @GetMapping("/orders")
    public Mono<ResponseEntity<Map<String, Object>>> orderServiceFallback() {
        log.warn("Order service fallback triggered");
        return createFallbackResponse("order-service", orderServiceFallback);
    }

    @GetMapping("/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> genericFallback(@PathVariable String service) {
        log.warn("Generic fallback triggered for service: {}", service);
        return createFallbackResponse(service, defaultFallback);
    }

    private Mono<ResponseEntity<Map<String, Object>>> createFallbackResponse(String service, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ERROR");
        response.put("service", service);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        response.put("retryable", true);

        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}
