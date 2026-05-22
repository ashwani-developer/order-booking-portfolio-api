package com.trading.calculator;

import com.trading.model.RiskFlag;
import net.jqwik.api.*;
import net.jqwik.api.Combinators;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OverlapCalculatorPropertyTest {

    // Feature: order-booking-portfolio-api, Property 7: Dice coefficient formula correctness
    // **Validates: Requirements 5.1, 5.7**

    @Property
    void diceOverlapMatchesFormulaForNonEmptySets(
            @ForAll("stockSets") Set<String> portfolio,
            @ForAll("stockSets") Set<String> basket) {

        double result = OverlapCalculator.computeDiceOverlap(portfolio, basket);

        if (portfolio.isEmpty() || basket.isEmpty()) {
            assertThat(result).isEqualTo(0.0);
        } else {
            Set<String> intersection = new HashSet<>(portfolio);
            intersection.retainAll(basket);
            double expected = (2.0 * intersection.size()) / (portfolio.size() + basket.size()) * 100.0;
            assertThat(result).isCloseTo(expected, within(0.0001));
        }
    }

    @Property
    void diceOverlapIsZeroWhenPortfolioIsEmpty(@ForAll("stockSets") Set<String> basket) {
        Set<String> emptyPortfolio = Collections.emptySet();

        double result = OverlapCalculator.computeDiceOverlap(emptyPortfolio, basket);

        assertThat(result).isEqualTo(0.0);
    }

    @Property
    void diceOverlapAlwaysInRange(
            @ForAll("stockSets") Set<String> portfolio,
            @ForAll("stockSets") Set<String> basket) {

        double result = OverlapCalculator.computeDiceOverlap(portfolio, basket);

        assertThat(result).isBetween(0.0, 100.0);
    }

    // Feature: order-booking-portfolio-api, Property 8: Risk Flag Threshold Correctness
    // **Validates: Requirements 5.3, 5.4, 5.5**

    @Property
    void riskFlagIsHighWhenMaxOverlapAtLeast60(@ForAll("highOverlapMaps") Map<String, Double> overlaps) {
        RiskFlag flag = OverlapCalculator.determineRiskFlag(overlaps);

        assertThat(flag).isEqualTo(RiskFlag.HIGH);
    }

    @Property
    void riskFlagIsMediumWhenMaxOverlapBetween40And60(@ForAll("mediumOverlapMaps") Map<String, Double> overlaps) {
        RiskFlag flag = OverlapCalculator.determineRiskFlag(overlaps);

        assertThat(flag).isEqualTo(RiskFlag.MEDIUM);
    }

    @Property
    void riskFlagIsLowWhenAllOverlapsBelow40(@ForAll("lowOverlapMaps") Map<String, Double> overlaps) {
        RiskFlag flag = OverlapCalculator.determineRiskFlag(overlaps);

        assertThat(flag).isEqualTo(RiskFlag.LOW);
    }

    @Property
    void riskFlagThresholdsAreConsistent(@ForAll("overlapMaps") Map<String, Double> overlaps) {
        RiskFlag flag = OverlapCalculator.determineRiskFlag(overlaps);
        double max = overlaps.values().stream().mapToDouble(d -> d).max().orElse(0.0);

        if (max >= 60.0) {
            assertThat(flag).isEqualTo(RiskFlag.HIGH);
        } else if (max >= 40.0) {
            assertThat(flag).isEqualTo(RiskFlag.MEDIUM);
        } else {
            assertThat(flag).isEqualTo(RiskFlag.LOW);
        }
    }

    // --- Providers ---

    @Provide
    Arbitrary<Set<String>> stockSets() {
        String[] symbols = {"AAPL", "MSFT", "GOOGL", "TSLA", "NVDA",
                "JPM", "GS", "BAC", "MS", "WFC",
                "XOM", "JNJ", "AMZN", "META", "NFLX",
                "DIS", "V", "MA", "HD", "PG"};
        return Arbitraries.of(symbols).set().ofMinSize(0).ofMaxSize(20);
    }

    @Provide
    Arbitrary<Map<String, Double>> overlapMaps() {
        Arbitrary<Double> values = Arbitraries.doubles().between(0.0, 100.0);
        return Arbitraries.maps(
                Arbitraries.of("TECH_HEAVY", "FINANCE_HEAVY", "BALANCED"),
                values
        ).ofMinSize(1).ofMaxSize(3);
    }

    @Provide
    Arbitrary<Map<String, Double>> highOverlapMaps() {
        // At least one value >= 60.0
        Arbitrary<Double> highValue = Arbitraries.doubles().between(60.0, 100.0);
        Arbitrary<Double> anyValue = Arbitraries.doubles().between(0.0, 100.0);
        return Combinators.combine(highValue, anyValue, anyValue).as((high, v2, v3) -> {
            Map<String, Double> map = new HashMap<>();
            map.put("TECH_HEAVY", high);
            map.put("FINANCE_HEAVY", v2);
            map.put("BALANCED", v3);
            return map;
        });
    }

    @Provide
    Arbitrary<Map<String, Double>> mediumOverlapMaps() {
        // Max value in [40.0, 60.0)
        Arbitrary<Double> mediumValue = Arbitraries.doubles().between(40.0, true, 60.0, false);
        Arbitrary<Double> lowValue = Arbitraries.doubles().between(0.0, true, 60.0, false);
        return Combinators.combine(mediumValue, lowValue, lowValue).as((med, v2, v3) -> {
            Map<String, Double> map = new HashMap<>();
            map.put("TECH_HEAVY", med);
            map.put("FINANCE_HEAVY", Math.min(v2, med));
            map.put("BALANCED", Math.min(v3, med));
            return map;
        });
    }

    @Provide
    Arbitrary<Map<String, Double>> lowOverlapMaps() {
        // All values < 40.0
        Arbitrary<Double> lowValue = Arbitraries.doubles().between(0.0, true, 40.0, false);
        return Combinators.combine(lowValue, lowValue, lowValue).as((v1, v2, v3) -> {
            Map<String, Double> map = new HashMap<>();
            map.put("TECH_HEAVY", v1);
            map.put("FINANCE_HEAVY", v2);
            map.put("BALANCED", v3);
            return map;
        });
    }
}
