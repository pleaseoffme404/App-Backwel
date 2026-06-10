package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.modules.users.entity.address.UserAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Range;

public record CreateAddressDTO(
        @NotBlank(message = "Address name is required and cannot be empty.")
        @Size(min = 3, max = 100, message = "Address name must be between {min} and {max} characters.")
        String addressName,

        @NotNull(message = "Slot index is required.")
        @Range(min = 0, max = 3, message = "Slot index must be between {min} and {max}.")
        Integer slotIndex,

        @NotNull(message = "Google address details are required.")
        @Valid
        GoogleAddressDTO googleAddressDTO
) {

        /**
         * Returns a non-persisted User Address Entity with the current parameters
         */
        public UserAddress toEntity() {
                return new UserAddress(slotIndex, addressName, googleAddressDTO.toEntity());
        }
}