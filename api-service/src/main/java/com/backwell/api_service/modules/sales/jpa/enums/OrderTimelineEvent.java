package com.backwell.api_service.modules.sales.jpa.enums;

public enum OrderTimelineEvent {
    CREATED,
    CANCEL_POST_CREATED,

    APPROVED,
    CANCEL_POST_APPROVED,

    SENT,
    CANCEL_POST_SENT,

    DELIVERED,
    RETURNED,
    CANCELLED,
}
