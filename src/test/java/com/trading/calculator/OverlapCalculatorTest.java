package com.trading.calculator;

import com.trading.model.RiskFlag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class OverlapCalculatorTest {

    @Test
    void computeDiceOverlap_knownValues() {
        // portfolio={AAPL, TSLA, NVDA}, basket=TECH_HEAVY={AAPL, MSFT, GOOGL, TSLA, NVDA}
        // intersection={AAPL, TSLA, NVDA} size=3
        // dice = 2*3/(3+5)*100 = 75.0
        Set<String> portfolio = Set.of("AAPL", "TSLA", "NVDA");
        Set<String> basket = Set.of("AAPL", "MSFT", "GOOGL", "TSLA", "NVDA");

        double result = OverlapCalculator.computeDiceOverlap(portfolio, basket);

        assertThat(result).isCloseTo(75.0, within(0.01));
    }

    @Test
    void computeDiceOverlap_emptyPortfolio() {
        Set<String> portfolio = Set.of();
        Set<String> basket = Set.of("AAPL", "MSFT");

        double result = OverlapCalculator.computeDiceOverlap(portfolio, basket);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void computeDiceOverlap_fullOverlap() {
        Set<String> stocks = Set.of("AAPL", "MSFT", "GOOGL");

        double result = OverlapCalculator.computeDiceOverlap(stocks, stocks);

        assertThat(result).isCloseTo(100.0, within(0.01));
    }

    @Test
    void computeDiceOverlap_noOverlap() {
        Set<String> portfolio = Set.of("XYZ", "ABC");
        Set<String> basket = Set.of("DEF", "GHI");

        double result = OverlapCalculator.computeDiceOverlap(portfolio, basket);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void computeOverlap_workedExample() {
        // portfolio={AAPL, TSLA, NVDA}
        // TECH_HEAVY={AAPL, MSFT, GOOGL, TSLA, NVDA} → intersection=3, dice=2*3/(3+5)*100=75%
        // FINANCE_HEAVY={JPM, GS, BAC, MS, WFC} → intersection=0, dice=0%
        // BALANCED={AAPL, JPM, XOM, JNJ, TSLA} → intersection=2, dice=2*2/(3+5)*100=50%
        Set<String> portfolio = Set.of("AAPL", "TSLA", "NVDA");

        OverlapResult result = OverlapCalculator.computeOverlap(portfolio);

        assertThat(result.basketOverlaps().get("TECH_HEAVY")).isCloseTo(75.0, within(0.01));
        assertThat(result.basketOverlaps().get("FINANCE_HEAVY")).isCloseTo(0.0, within(0.01));
        assertThat(result.basketOverlaps().get("BALANCED")).isCloseTo(50.0, within(0.01));
        assertThat(result.riskFlag()).isEqualTo(RiskFlag.HIGH);
        assertThat(result.dominantBasket()).isEqualTo("TECH_HEAVY");
    }

    @Test
    void computeOverlap_emptyPortfolio() {
        Set<String> portfolio = Set.of();

        OverlapResult result = OverlapCalculator.computeOverlap(portfolio);

        assertThat(result.basketOverlaps().get("TECH_HEAVY")).isEqualTo(0.0);
        assertThat(result.basketOverlaps().get("FINANCE_HEAVY")).isEqualTo(0.0);
        assertThat(result.basketOverlaps().get("BALANCED")).isEqualTo(0.0);
        assertThat(result.riskFlag()).isEqualTo(RiskFlag.LOW);
    }

    @Test
    void determineRiskFlag_high() {
        Map<String, Double> overlaps = Map.of("TECH_HEAVY", 60.0, "FINANCE_HEAVY", 10.0);

        RiskFlag result = OverlapCalculator.determineRiskFlag(overlaps);

        assertThat(result).isEqualTo(RiskFlag.HIGH);
    }

    @Test
    void determineRiskFlag_medium() {
        Map<String, Double> overlaps = Map.of("TECH_HEAVY", 45.0, "FINANCE_HEAVY", 10.0);

        RiskFlag result = OverlapCalculator.determineRiskFlag(overlaps);

        assertThat(result).isEqualTo(RiskFlag.MEDIUM);
    }

    @Test
    void determineRiskFlag_low() {
        Map<String, Double> overlaps = Map.of("TECH_HEAVY", 30.0, "FINANCE_HEAVY", 10.0);

        RiskFlag result = OverlapCalculator.determineRiskFlag(overlaps);

        assertThat(result).isEqualTo(RiskFlag.LOW);
    }
}
