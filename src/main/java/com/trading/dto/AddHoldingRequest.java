package com.trading.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record AddHoldingRequest(
    @NotBlank String stock,
    @NotBlank String sector,
    @Positive int quantity
) {}
