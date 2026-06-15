package com.backwell.api_service.modules.credit.controller.res;

import com.backwell.api_service.modules.credit.entity.UserCredit;

import java.math.BigDecimal;
import java.util.UUID;

public record CreditBalanceDTO(
        UUID userId,
        BigDecimal balance
) {

    public static CreditBalanceDTO fromEntity(UserCredit entity) {
        return new CreditBalanceDTO(entity.getUserId(), entity.getBalance());
    }
}
