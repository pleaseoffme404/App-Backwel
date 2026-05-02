package com.backwell.api_service.modules.users.dto;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest (
        @NotNull
        @Min(0)
        @Max(3)
        Integer slotIndex,

        @Size(min = 3, max = 100)
        String addressName,

        @Valid
        GoogleAddressDTO googleAddressDTO
) {
        public UpdateAddressRequest {
                if (addressName == null && googleAddressDTO == null) {
                        throw new IllegalArgumentException("addressName or googleAddressDTO is null");
                }
        }
}
