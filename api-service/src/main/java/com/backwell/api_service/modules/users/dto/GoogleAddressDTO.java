package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.modules.users.entity.address.GoogleAddress;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record GoogleAddressDTO(
        @NotBlank(message = "Google Place ID is required.")
        @Size(max = 512, message = "Place ID cannot exceed {max} characters.")
        String placeId,

        @NotBlank(message = "Formatted address is required.")
        @Size(max = 1000, message = "Formatted address cannot exceed {max} characters.")
        String formattedAddress,

        @NotBlank(message = "Street number is required.")
        @Size(max = 100, message = "Street number cannot exceed {max} characters.")
        String streetNumber,

        @NotBlank(message = "Route/Street name is required.")
        @Size(max = 100, message = "Route cannot exceed {max} characters.")
        String route,

        @NotBlank(message = "Locality/City is required.")
        @Size(max = 100, message = "Locality cannot exceed {max} characters.")
        String locality,

        @NotBlank(message = "Administrative area level 1 (State/Province) is required.")
        @Size(max = 100, message = "Administrative area level 1 cannot exceed {max} characters.")
        String administrativeAreaLevel1,

        @NotBlank(message = "Postal code is required.")
        @Size(max = 10, message = "Postal code cannot exceed {max} characters.")
        String postalCode,

        @NotBlank(message = "Country code is required.")
        @Size(max = 100, message = "Country code cannot exceed {max} characters.")
        String countryCode,

        @NotNull(message = "Latitude is required.")
        @DecimalMin(value = "-90.0", message = "Latitude must be greater than or equal to -90.0.")
        @DecimalMax(value = "90.0", message = "Latitude must be less than or equal to 90.0.")
        BigDecimal latitude,

        @NotNull(message = "Longitude is required.")
        @DecimalMin(value = "-180.0", message = "Longitude must be greater than or equal to -180.0.")
        @DecimalMax(value = "180.0", message = "Longitude must be less than or equal to 180.0.")
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