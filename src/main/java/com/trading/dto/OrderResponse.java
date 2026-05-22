package com.trading.dto;

import com.trading.model.Order;
import com.trading.model.OrderState;
import com.trading.model.Side;

import java.time.LocalDateTime;

public record OrderResponse(
    Long id,
    String traderId,
    String stock,
    String sector,
    int quantity,
    Side side,
    OrderState state,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
            order.getId(),
            order.getTraderId(),
            order.getStock(),
            order.getSector(),
            order.getQuantity(),
            order.getSide(),
            order.getState(),
            order.getCreatedAt(),
            order.getUpdatedAt()
        );
    }
}
