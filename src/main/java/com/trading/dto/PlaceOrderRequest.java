package com.trading.dto;

import com.trading.model.Side;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlaceOrderRequest(
    @NotBlank String traderId,
    @NotBlank String stock,
    @NotBlank String sector,
    @Positive int quantity,
    @NotNull Side side
) {}
