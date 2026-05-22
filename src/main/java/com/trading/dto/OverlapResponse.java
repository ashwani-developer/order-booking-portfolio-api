package com.trading.dto;

import com.trading.model.RiskFlag;

import java.util.List;

public record OverlapResponse(
    List<BasketOverlap> overlaps,
    String dominantBasket,
    RiskFlag riskFlag
) {}
