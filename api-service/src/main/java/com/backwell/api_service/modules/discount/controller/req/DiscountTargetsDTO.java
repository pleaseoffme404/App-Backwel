package com.backwell.api_service.modules.discount.controller.req;

import com.backwell.api_service.validators.AtLeastOneNotNull;
import jakarta.validation.constraints.Size;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@AtLeastOneNotNull
public record DiscountTargetsDTO(
        @Size(max = 100)
        Set<UUID> itemTargets,

        @Size(max = 100)
        Set<UUID> productTargets,

        @Size(max = 100)
        Set<UUID> categoryTargets
) {
        public Optional<Set<UUID>> getItemTargets() {
                return Optional.ofNullable(itemTargets);
        }
        public Optional<Set<UUID>> getProductTargets() {
                return Optional.ofNullable(productTargets);
        }
        public Optional<Set<UUID>> getCategoryTargets() {
                return Optional.ofNullable(categoryTargets);
        }
}
