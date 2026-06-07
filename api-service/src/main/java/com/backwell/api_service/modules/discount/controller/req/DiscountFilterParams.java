package com.backwell.api_service.modules.discount.controller.req;

import com.backwell.api_service.modules.discount.enums.DiscountSortField;
import lombok.Builder;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Builder
public record DiscountFilterParams(
        UUID discountId,
        Boolean nowActive,
        Boolean stackable,
        BigDecimal decimalValueMin,
        BigDecimal decimalValueMax,
        List<UUID> targetingItems,
        List<UUID> targetingProducts,
        List<UUID> targetingCategories,
        Instant startDateMin,
        Instant startDateMax,
        Instant endDateMin,
        Instant endDateMax,
        Instant createdAtMin,
        Instant createdAtMax,

        // Not null guaranteed fields
        DiscountSortField sortField,
        Sort.Direction direction,
        int pageSize,
        int pageNumber

) {
    // --- Getters personalizados que retornan Optional<T> ---

    public Optional<UUID> getDiscountId() {
        return Optional.ofNullable(discountId);
    }

    public Optional<Boolean> getNowActive() {
        return Optional.ofNullable(nowActive);
    }

    public Optional<Boolean> getStackable() {
        return Optional.ofNullable(stackable);
    }

    public Optional<BigDecimal> getDecimalValueMin() {
        return Optional.ofNullable(decimalValueMin);
    }

    public Optional<BigDecimal> getDecimalValueMax() {
        return Optional.ofNullable(decimalValueMax);
    }

    public Optional<List<UUID>> getTargetingItems() {
        return Optional.ofNullable(targetingItems);
    }

    public Optional<List<UUID>> getTargetingProducts() {
        return Optional.ofNullable(targetingProducts);
    }

    public Optional<List<UUID>> getTargetingCategories() {
        return Optional.ofNullable(targetingCategories);
    }

    public Optional<Instant> getStartDateMin() {
        return Optional.ofNullable(startDateMin);
    }

    public Optional<Instant> getStartDateMax() {
        return Optional.ofNullable(startDateMax);
    }

    public Optional<Instant> getEndDateMin() {
        return Optional.ofNullable(endDateMin);
    }

    public Optional<Instant> getEndDateMax() {
        return Optional.ofNullable(endDateMax);
    }

    public Optional<Instant> getCreatedAtMin() {
        return Optional.ofNullable(createdAtMin);
    }

    public Optional<Instant> getCreatedAtMax() {
        return Optional.ofNullable(createdAtMax);
    }
}