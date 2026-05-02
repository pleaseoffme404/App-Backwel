package com.backwell.api_service.modules.inventory.service;

import com.backwell.api_service.common.config.RedisSpacePrefixes;
import com.backwell.api_service.common.exception.SystemException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisStockService {
    private final RedisSpacePrefixes redisPrefixes;
    private final RedisTemplate<String, Integer> integerRedisTemplate;

    public void saveInitialStock(UUID itemId, int quantity) {
        Assert.notNull(itemId, "ItemId can't be null");
        Assert.isTrue(quantity >= 0, "Quantity must not be negative");

        integerRedisTemplate.opsForValue().set(buildKey(itemId), quantity);
    }

    public Optional<Integer> get(UUID itemId) {
        return Optional.ofNullable(integerRedisTemplate.opsForValue().get(buildKey(itemId)));
    }

    public int getOrThrow(UUID itemId) {
        return get(itemId)
                .orElseThrow(() -> new SystemException("No cache value found for product `%s`".formatted(itemId)));
    }

    public Long increment(UUID itemId, int delta) {
        return integerRedisTemplate.opsForValue().increment(buildKey(itemId), delta);
    }

    private String buildKey(UUID itemId) {
        return String.join(":", redisPrefixes.getStock(), itemId.toString());
    }
}
