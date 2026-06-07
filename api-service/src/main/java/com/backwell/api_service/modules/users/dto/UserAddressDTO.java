package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.modules.users.entity.address.GoogleAddress;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.Length;

public record UserAddressDTO(
        @NotBlank
        @Length(min = 2, max = 100)
        @Pattern(regexp = "^[\\p{L}]+(?:[ '-][\\p{L}]+)*$",
                message = "Name contains forbidden characters.")
        String addressName,

        @NotNull
        @Min(0)
        @Max(3)
        Integer slotIndex,

        @NotNull
        @Valid
        GoogleAddressDTO googleAddressDTO
) {
    public UserAddressDTO (String addressName, int slotIndex, GoogleAddress googleAddress){
        this(addressName, slotIndex, GoogleAddressDTO.fromEntity(googleAddress));
    }
}
