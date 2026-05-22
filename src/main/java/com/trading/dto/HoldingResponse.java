package com.trading.dto;

import com.trading.model.Holding;

public record HoldingResponse(
    String traderId,
    String stock,
    String sector,
    int quantity
) {
    public static HoldingResponse from(Holding holding) {
        return new HoldingResponse(
            holding.getTraderId(),
            holding.getStock(),
            holding.getSector(),
            holding.getQuantity()
        );
    }
}
