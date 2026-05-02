package com.backwell.api_service.modules.users.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Embeddable
@AllArgsConstructor
@NoArgsConstructor
@Data
public class GoogleAddress {
    @Column(length = 512)
    private String place_id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String formattedAddress;

    private String streetNumber;

    private String route;

    private String locality;

    private String administrativeAreaLevel1;

    @Column(length = 10)
    private String postalCode;

    @Column(length = 2, nullable = false)
    private String countryCode;

    @Column(precision = 11,scale = 8)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 8)
    private BigDecimal longitude;
}
