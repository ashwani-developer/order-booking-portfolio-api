package com.trading.controller;

import com.trading.calculator.OverlapCalculator;
import com.trading.calculator.OverlapResult;
import com.trading.dto.AddHoldingRequest;
import com.trading.dto.BasketOverlap;
import com.trading.dto.HoldingResponse;
import com.trading.dto.OverlapResponse;
import com.trading.dto.PortfolioResponse;
import com.trading.model.Holding;
import com.trading.service.PortfolioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/{traderId}")
    public ResponseEntity<PortfolioResponse> getPortfolio(@PathVariable String traderId) {
        PortfolioResponse response = portfolioService.getPortfolio(traderId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{traderId}/add")
    public ResponseEntity<HoldingResponse> addToPortfolio(
            @PathVariable String traderId, @Valid @RequestBody AddHoldingRequest request) {
        Holding holding = portfolioService.addHolding(
            traderId, request.stock(), request.sector(), request.quantity());
        return ResponseEntity.ok(HoldingResponse.from(holding));
    }

    @GetMapping("/{traderId}/overlap")
    public ResponseEntity<OverlapResponse> getOverlap(@PathVariable String traderId) {
        // Get portfolio stocks
        PortfolioResponse portfolio = portfolioService.getPortfolio(traderId);
        Set<String> stocks = portfolio.positions().keySet();

        // Compute overlap using pure Java calculator
        OverlapResult result = OverlapCalculator.computeOverlap(stocks);

        // Format response with overlap percentages as "XX.XX%"
        List<BasketOverlap> overlaps = result.basketOverlaps().entrySet().stream()
            .map(e -> new BasketOverlap(e.getKey(), String.format("%.2f%%", e.getValue())))
            .toList();

        OverlapResponse response = new OverlapResponse(overlaps, result.dominantBasket(), result.riskFlag());
        return ResponseEntity.ok(response);
    }
}
