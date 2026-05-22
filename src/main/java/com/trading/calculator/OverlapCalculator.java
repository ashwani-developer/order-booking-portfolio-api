package com.trading.calculator;

import com.trading.model.RiskFlag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OverlapCalculator {

    private static final Set<String> TECH_HEAVY = Set.of("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA");
    private static final Set<String> FINANCE_HEAVY = Set.of("JPM", "GS", "BAC", "MS", "WFC");
    private static final Set<String> BALANCED = Set.of("AAPL", "JPM", "XOM", "JNJ", "TSLA");

    private static final Map<String, Set<String>> BASKETS = Map.of(
        "TECH_HEAVY", TECH_HEAVY,
        "FINANCE_HEAVY", FINANCE_HEAVY,
        "BALANCED", BALANCED
    );

    public static double computeDiceOverlap(Set<String> portfolioStocks, Set<String> basketStocks) {
        if (portfolioStocks.isEmpty() || basketStocks.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(portfolioStocks);
        intersection.retainAll(basketStocks);

        int intersectionSize = intersection.size();
        return (2.0 * intersectionSize) / (portfolioStocks.size() + basketStocks.size()) * 100.0;
    }

    public static OverlapResult computeOverlap(Set<String> portfolioStocks) {
        Map<String, Double> overlaps = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : BASKETS.entrySet()) {
            double overlap = computeDiceOverlap(portfolioStocks, entry.getValue());
            overlaps.put(entry.getKey(), overlap);
        }

        String dominantBasket = overlaps.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse("");

        RiskFlag riskFlag = determineRiskFlag(overlaps);

        return new OverlapResult(overlaps, dominantBasket, riskFlag);
    }

    public static RiskFlag determineRiskFlag(Map<String, Double> overlaps) {
        double maxOverlap = overlaps.values().stream()
            .mapToDouble(Double::doubleValue)
            .max()
            .orElse(0.0);

        if (maxOverlap >= 60.0) {
            return RiskFlag.HIGH;
        } else if (maxOverlap >= 40.0) {
            return RiskFlag.MEDIUM;
        } else {
            return RiskFlag.LOW;
        }
    }
}
