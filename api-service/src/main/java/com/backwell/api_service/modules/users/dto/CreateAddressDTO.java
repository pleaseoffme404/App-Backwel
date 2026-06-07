package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.modules.users.entity.address.UserAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record CreateAddressDTO(
        @NotBlank
        @Size(min = 3, max = 100)
        String addressName,

        @Min(0)
        @Max(3)
        Integer slotIndex,

        @NotNull
        @Valid
        GoogleAddressDTO googleAddressDTO
) {

        /**
         * Returns a non persisted User Address Entity with the current parameters*/
        public UserAddress toEntity() {
                return new UserAddress(slotIndex, addressName, googleAddressDTO.toEntity());
        }
}
