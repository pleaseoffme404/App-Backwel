package com.backwell.api_service.modules.users.dto;

import com.backwell.api_service.modules.users.entity.UserInfo;

import java.util.UUID;


public record UserInfoDTO(
        UUID id,
        String email,
        String role,

        String name,
        String surname,
        String phoneNumber,
        String pictureUrl,

        String countryCode,
        String currencyCode
) {
    public static UserInfoDTO fromEntity(UserInfo u, String role) {
        return new UserInfoDTO(
                u.getUuid(),
                u.getEmail(),
                role,
                u.getName(),
                u.getSurname(),
                u.getPhoneNumber(),
                u.getPictureUrl(),
                u.getCountryCode(),
                u.getCurrencyCode()
        );
    }
}
