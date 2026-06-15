package com.backwell.api_service.common.idempotency;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum IdempotencyDomain {
    SALES("sales"),
    CREDIT("credit");
    private final String suffix;

    public String suffix() {
        return suffix;
    }
}
