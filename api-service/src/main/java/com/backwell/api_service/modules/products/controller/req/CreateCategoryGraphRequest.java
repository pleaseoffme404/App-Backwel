package com.backwell.api_service.modules.products.controller.req;


import com.backwell.api_service.modules.products.dto.CategoryNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Getter
@Jacksonized
@Builder
public class CreateCategoryGraphRequest {

    private final UUID parentId;

    @NotNull
    @NotEmpty
    @Valid
    private final List<@NotNull CategoryNode> nodes;

    public Optional<UUID> getParentId() {
        return Optional.ofNullable(parentId);
    }
}
