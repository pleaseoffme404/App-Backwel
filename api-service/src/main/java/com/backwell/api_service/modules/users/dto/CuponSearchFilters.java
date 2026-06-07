package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.modules.users.entity.cupon.CuponType;
import com.backwell.api_service.validators.AtLeastOneNotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@AtLeastOneNotNull
public record CuponSearchFilters (
        UUID lastId,
        Integer pageSize,

        @Size(min = 1, max = 100)
        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Cupon Name contains forbidden characters.")
        String nameLike,

        CuponType type,

        UUID targetId,

        @DecimalMin("0.0")
        @DecimalMax("100.0")
        BigDecimal percentageMin,

        @DecimalMin("0.0")
        @DecimalMax("100.0")
        BigDecimal percentageMax,

        Boolean active,
        Boolean stackable,

        Instant createdAtMin,
        Instant createdAtMax,

        Instant usedAtMin,
        Instant usedAtMax
) {
    public Optional<UUID> optLastId() { return Optional.ofNullable(lastId); }
    public Optional<String> optNameLike() { return Optional.ofNullable(nameLike); }
    public Optional<CuponType> optType() { return Optional.ofNullable(type); }
    public Optional<UUID> optTargetId() { return Optional.ofNullable(targetId); }
    public Optional<BigDecimal> optPercentageMin() { return Optional.ofNullable(percentageMin); }
    public Optional<BigDecimal> optPercentageMax() { return Optional.ofNullable(percentageMax); }
    public Optional<Boolean> optActive() { return Optional.ofNullable(active); }
    public Optional<Boolean> optStackable() { return Optional.ofNullable(stackable); }
    public Optional<Instant> optCreatedAtMin() { return Optional.ofNullable(createdAtMin); }
    public Optional<Instant> optCreatedAtMax() { return Optional.ofNullable(createdAtMax); }
    public Optional<Instant> optUsedAtMin() { return Optional.ofNullable(usedAtMin); }
    public Optional<Instant> optUsedAtMax() { return Optional.ofNullable(usedAtMax); }
}
