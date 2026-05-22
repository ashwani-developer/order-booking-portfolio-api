package com.trading.calculator;

import com.trading.model.RiskFlag;

import java.util.Map;

public record OverlapResult(
    Map<String, Double> basketOverlaps,
    String dominantBasket,
    RiskFlag riskFlag
) {}
