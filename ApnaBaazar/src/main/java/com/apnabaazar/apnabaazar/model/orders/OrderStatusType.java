package com.apnabaazar.apnabaazar.model.orders;

    public enum OrderStatusType {
        ORDER_PLACED,
        ORDER_CONFIRMED,
        ORDER_REJECTED,
        CANCELLED,
        ORDER_SHIPPED,
        DELIVERED,
        RETURN_REQUESTED,
        RETURN_REJECTED,
        RETURN_APPROVED,
        PICK_UP_INITIATED,
        PICK_UP_COMPLETED,
        REFUND_INITIATED,
        REFUND_COMPLETED,
        CLOSED
    }