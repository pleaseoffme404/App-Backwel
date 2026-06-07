package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.modules.users.entity.address.GoogleAddress;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GoogleAddressDTO(
        @Size(max = 512)
        @NotNull
        String placeId,

        @Size(max = 1000)
        @NotNull
        String formattedAddress,

        @Size(max = 100)
        @NotNull
        String streetNumber,

        @Size(max = 100)
        @NotNull
        String route,

        @Size(max = 100)
        @NotNull
        String locality,

        @NotNull
        @Size(max = 100)
        String administrativeAreaLevel1,

        @NotNull
        @Size(max = 10)
        String postalCode,

        @NotNull
        @Size(max = 100)
        String countryCode,

        @NotNull
        BigDecimal latitude,

        @NotNull
        BigDecimal longitude
) {

    public GoogleAddress toEntity(){
        return new GoogleAddress(
                placeId,
                formattedAddress,
                streetNumber,
                route,
                locality,
                administrativeAreaLevel1,
                postalCode,
                countryCode,
                latitude,
                longitude
        );
    }

    public static GoogleAddressDTO fromEntity(GoogleAddress e){
        return new GoogleAddressDTO(
                e.getPlace_id(),
                e.getFormattedAddress(),
                e.getStreetNumber(),
                e.getRoute(),
                e.getLocality(),
                e.getAdministrativeAreaLevel1(),
                e.getPostalCode(),
                e.getCountryCode(),
                e.getLatitude(),
                e.getLongitude()
        );
    }
}
