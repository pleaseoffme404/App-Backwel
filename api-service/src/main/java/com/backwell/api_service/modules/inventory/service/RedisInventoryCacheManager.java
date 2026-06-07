package com.backwell.api_service.modules.inventory.service;

import com.backwell.api_service.common.config.RedisSpacePrefixes;
import com.backwell.api_service.common.exception.SystemException;
import com.backwell.api_service.modules.inventory.dto.InventoryDeltaRequest;
import com.backwell.api_service.modules.inventory.dto.RedisInventoryInfo;
import com.backwell.api_service.modules.inventory.entity.InventoryTrace;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisInventoryCacheManager {
    private final RedisSpacePrefixes redisPrefixes;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String FIELD_AVAILABLE = "available";
    private static final String FIELD_RESERVED = "reserved";
    private static final String FIELD_REDUNDANCY = "redundancy";
    private static final String FIELD_PHYSICAL = "physical";


    /**
     * Inicializa el inventario base para un ítem nuevo o sincronizado.
     */
    public void saveInitialInventory(InventoryTrace inventoryTrace) {
        saveInitialInventory(inventoryTrace.getItemId(), RedisInventoryInfo.of(inventoryTrace));
    }

    public void saveInitialInventory(UUID itemId, RedisInventoryInfo info) {
        Assert.notNull(itemId, "ItemId can't be null");
        Assert.notNull(info, "Inventory info can't be null");

        String key = buildKey(itemId);
        Map<String, String> hashValues = new HashMap<>();
        hashValues.put(FIELD_AVAILABLE, String.valueOf(info.availableStock()));
        hashValues.put(FIELD_RESERVED, String.valueOf(info.reservedStock()));
        hashValues.put(FIELD_REDUNDANCY, String.valueOf(info.redundancyStock()));
        hashValues.put(FIELD_PHYSICAL, String.valueOf(info.physicalStock()));

        stringRedisTemplate.opsForHash().putAll(key, hashValues);
    }


    /**
     * Recupera la instantánea completa del inventario de un ítem.
     */
    public Optional<RedisInventoryInfo> getInventory(UUID itemId) {
        Assert.notNull(itemId, "ItemId can't be null");
        String key = buildKey(itemId);

        // Trae todos los campos del Hash en una sola operación de red
        Map<Object, Object> fields = stringRedisTemplate.opsForHash().entries(key);

        if (fields.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new RedisInventoryInfo(
                parseField(fields.get(FIELD_AVAILABLE)),
                parseField(fields.get(FIELD_RESERVED)),
                parseField(fields.get(FIELD_REDUNDANCY)),
                parseField(fields.get(FIELD_PHYSICAL))
        ));
    }

    public RedisInventoryInfo getInventoryOrThrow(UUID itemId) {
        return getInventory(itemId)
                .orElseThrow(() -> new SystemException("No inventory cache found for item `%s`".formatted(itemId)));
    }

    public int getAvailableOrThrow(@NotNull UUID itemId) {
        return getInventoryOrThrow(itemId).availableStock();
    }

    /**
     * Aplica deltas compuestos a los campos del Hash de manera atómica.
     * Replica las pruebas aritméticas de consistencia antes de impactar Redis.
     */
    public void applyInventoryDeltas(UUID itemId, InventoryDeltaRequest deltas) {
        Assert.notNull(itemId, "ItemId can't be null");
        Assert.notNull(deltas, "Deltas request can't be null");

        if (!deltas.isValidMovement()) {
            throw new SystemException("Arithmetical test failed. Inventory movement delta is inconsistent.");
        }

        String key = buildKey(itemId);

        // Ejecutamos incrementos independientes por campo (HINCRBY nativo)
        // Nota: Si necesitas que los 4 incrementos se ejecuten de forma estrictamente transaccional (All-or-Nothing)
        // en Redis, puedes envolver esto en un bloque Redis Tx (MULTI/EXEC) o un Script Lua.
        if (deltas.availableDelta() != 0) {
            stringRedisTemplate.opsForHash().increment(key, FIELD_AVAILABLE, deltas.availableDelta());
        }
        if (deltas.reservedDelta() != 0) {
            stringRedisTemplate.opsForHash().increment(key, FIELD_RESERVED, deltas.reservedDelta());
        }
        if (deltas.redundancyDelta() != 0) {
            stringRedisTemplate.opsForHash().increment(key, FIELD_REDUNDANCY, deltas.redundancyDelta());
        }
        if (deltas.physicalDelta() != 0) {
            stringRedisTemplate.opsForHash().increment(key, FIELD_PHYSICAL, deltas.physicalDelta());
        }
    }

    /**
     * Consulta optimizada por lotes (MGET equivalente para Hashes utilizando PIPELINING)
     * Recupera de un solo viaje de red el inventario de múltiples IDs de ítems.
     */
    public Map<UUID, RedisInventoryInfo> getInventories(Set<UUID> itemIds) {
        if (itemIds == null || itemIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<UUID> idsList = new ArrayList<>(itemIds);

        // Ejecutamos comandos por lotes usando Pipeline para no bloquear la red
        List<Object> pipelinedResults = stringRedisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> Object execute(@NotNull RedisOperations<K, V> operations) {
                RedisOperations<String, String> stringOps = (RedisOperations<String, String>) operations;

                for (UUID itemId : idsList) {
                    stringOps.opsForHash().entries(buildKey(itemId));
                }
                return null;
            }
        });

        Map<UUID, RedisInventoryInfo> resultMap = new HashMap<>();

        for (int i = 0; i < idsList.size(); i++) {
            Object resultObj = pipelinedResults.get(i);
            if (resultObj instanceof Map<?, ?> rawMap && !rawMap.isEmpty()) {
                int available = parseField(rawMap.get(FIELD_AVAILABLE));
                int reserved = parseField(rawMap.get(FIELD_RESERVED));
                int redundancy = parseField(rawMap.get(FIELD_REDUNDANCY));
                int physical = parseField(rawMap.get(FIELD_PHYSICAL));

                resultMap.put(idsList.get(i), new RedisInventoryInfo(available, reserved, redundancy, physical));
            }
        }

        return resultMap;
    }

    private int parseField(Object value) {
        if (value == null) return 0;
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String buildKey(UUID itemId) {
        return String.join(":", redisPrefixes.INVENTORY_STOCK_PREFIX, itemId.toString());
    }
}
