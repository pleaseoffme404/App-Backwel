package com.backwell.api_service.common.idempotency;

import com.backwell.api_service.common.config.RedisSpacePrefixes;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Redis-backed cache used to coordinate idempotent operations across the
 * application.
 *
 * <p>
 * Each idempotent request is uniquely identified by the combination of:
 * </p>
 * <ul>
 *     <li>An {@link IdempotencyDomain} representing the functional area.</li>
 *     <li>The identifier of the user performing the operation.</li>
 *     <li>A client-provided idempotency key.</li>
 * </ul>
 *
 * <p>
 * When a request starts, an entry is atomically created in Redis with the
 * value {@link #STATE_IN_PROGRESS}. Once the operation completes
 * successfully, the value is updated to {@link #STATE_COMPLETED}. This
 * prevents duplicate processing caused by retries, network failures, or
 * repeated client submissions.
 * </p>
 *
 * <p>
 * Redis keys are generated using the following format:
 * <pre>{@code
 * <idempotencyPrefix>:<domain>:<userId>:<idempotencyKey>
 * }</pre>
 * where {@code <idempotencyPrefix>} is provided by
 * {@link RedisSpacePrefixes} and {@code <domain>} corresponds to the
 * suffix returned by {@link IdempotencyDomain#suffix()}.
 * </p>
 */
@Component
public class GlobalIdempotencyCache {

    /**
     * State indicating that the associated request is currently being processed.
     */
    public static final String STATE_IN_PROGRESS = "IN_PROGRESS";

    /**
     * State indicating that the associated request has completed successfully.
     */
    public static final String STATE_COMPLETED = "COMPLETED";

    private final StringRedisTemplate redisTemplate;

    /**
     * Namespace prefix used for all idempotency entries stored in Redis.
     */
    private final String idempotencyPrefix;

    /**
     * Creates a new {@code GlobalIdempotencyCache}.
     *
     * @param redisTemplate Redis template used to perform key-value operations.
     * @param spacePrefixes configuration object containing Redis namespace
     *                      prefixes.
     */
    public GlobalIdempotencyCache(
            StringRedisTemplate redisTemplate,
            RedisSpacePrefixes spacePrefixes
    ) {
        this.redisTemplate = redisTemplate;
        this.idempotencyPrefix = spacePrefixes.IDEMPOTENCY_PREFIX;
    }

    /**
     * Attempts to register a new idempotent request.
     *
     * <p>
     * A Redis key is created with the value
     * {@link #STATE_IN_PROGRESS} only if it does not already exist.
     * This operation relies on Redis {@code SETNX} semantics, making it
     * atomic and safe for concurrent requests.
     * </p>
     *
     * @param domain the functional domain to which the operation belongs.
     * @param userId the identifier of the user performing the operation.
     * @param key the client-provided idempotency key.
     * @param ttl the time-to-live for the Redis entry.
     * @return {@code true} if the request was successfully registered and
     *         should proceed, or {@code false} if an entry with the same
     *         identifiers already exists.
     * @throws IllegalArgumentException if any of the identifiers is
     *         {@code null}.
     */
    public boolean startRequest(
            IdempotencyDomain domain,
            UUID userId,
            UUID key,
            Duration ttl
    ) {
        String finalKey = buildKey(domain, userId, key);
        Boolean success = redisTemplate.opsForValue()
                .setIfAbsent(finalKey, STATE_IN_PROGRESS, ttl);
        return Boolean.TRUE.equals(success);
    }

    /**
     * Marks a previously registered request as completed.
     *
     * <p>
     * The Redis value is overwritten with
     * {@link #STATE_COMPLETED}, and the entry's TTL is refreshed
     * using the supplied duration.
     * </p>
     *
     * @param domain the functional domain to which the operation belongs.
     * @param userId the identifier of the user performing the operation.
     * @param key the client-provided idempotency key.
     * @param ttl the time-to-live for the completed state.
     * @throws IllegalArgumentException if any of the identifiers is
     *         {@code null}.
     */
    public void completeRequest(
            IdempotencyDomain domain,
            UUID userId,
            UUID key,
            Duration ttl
    ) {
        String finalKey = buildKey(domain, userId, key);
        redisTemplate.opsForValue().set(finalKey, STATE_COMPLETED, ttl);
    }

    /**
     * Retrieves the current status associated with an idempotent request.
     *
     * @param domain the functional domain to which the operation belongs.
     * @param userId the identifier of the user performing the operation.
     * @param key the client-provided idempotency key.
     * @return the stored state ({@link #STATE_IN_PROGRESS} or
     *         {@link #STATE_COMPLETED}), or {@code null} if no entry
     *         exists.
     * @throws IllegalArgumentException if any of the identifiers is
     *         {@code null}.
     */
    public String getRequestStatus(
            IdempotencyDomain domain,
            UUID userId,
            UUID key
    ) {
        String finalKey = buildKey(domain, userId, key);
        return redisTemplate.opsForValue().get(finalKey);
    }

    /**
     * Removes the Redis entry associated with the given idempotent request.
     *
     * @param domain the functional domain to which the operation belongs.
     * @param userId the identifier of the user performing the operation.
     * @param key the client-provided idempotency key.
     * @throws IllegalArgumentException if any of the identifiers is
     *         {@code null}.
     */
    public void removeKey(
            IdempotencyDomain domain,
            UUID userId,
            UUID key
    ) {
        String finalKey = buildKey(domain, userId, key);
        redisTemplate.delete(finalKey);
    }

    /**
     * Builds the Redis key that uniquely identifies an idempotent request.
     *
     * @param domain the operation domain.
     * @param userId the user identifier.
     * @param key the idempotency key.
     * @return the fully qualified Redis key in the format
     *         {@code <idempotencyPrefix>:<domain>:<userId>:<idempotencyKey>}.
     * @throws IllegalArgumentException if any argument is {@code null}.
     */
    private String buildKey(
            IdempotencyDomain domain,
            UUID userId,
            UUID key
    ) {
        if (domain == null || userId == null || key == null) {
            throw new IllegalArgumentException(
                    "At least one of the key components is null."
            );
        }

        return String.join(
                ":",
                idempotencyPrefix,
                domain.suffix(),
                userId.toString(),
                key.toString()
        );
    }
}