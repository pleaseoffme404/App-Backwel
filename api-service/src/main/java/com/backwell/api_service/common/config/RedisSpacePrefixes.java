package com.backwell.api_service.common.config;


import org.springframework.stereotype.Component;

@Component
public class RedisSpacePrefixes {
    public final String IDEMPOTENCY_PREFIX = "idempotency:";
    public final String INVITATION_EMAIL_INDEX_PREFIX = "invitation:pending_email:";
    public final String INVITATION_CODE_PREFIX = "invitation:code:";
    public final String INVENTORY_STOCK_PREFIX = "inventory:stock:";
}
