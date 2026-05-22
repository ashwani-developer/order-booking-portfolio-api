package com.trading.service;

import com.trading.dto.PortfolioResponse;
import com.trading.model.Holding;
import com.trading.model.Side;
import com.trading.repository.HoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PortfolioService {

    private final HoldingRepository holdingRepository;

    public PortfolioService(HoldingRepository holdingRepository) {
        this.holdingRepository = holdingRepository;
    }

    /**
     * Returns portfolio with holdings map and sector breakdown.
     * Returns empty maps if no holdings found for the trader.
     */
    public PortfolioResponse getPortfolio(String traderId) {
        List<Holding> holdings = holdingRepository.findByTraderId(traderId);

        Map<String, Integer> positions = new HashMap<>();
        Map<String, Integer> sectorBreakdown = new HashMap<>();

        for (Holding holding : holdings) {
            positions.put(holding.getStock(), holding.getQuantity());
            sectorBreakdown.merge(holding.getSector(), holding.getQuantity(), Integer::sum);
        }

        return new PortfolioResponse(traderId, positions, sectorBreakdown);
    }

    /**
     * Adds quantity to a holding (upsert semantics).
     * Uses pessimistic lock on the holding row to prevent lost updates.
     */
    @Transactional
    public Holding addHolding(String traderId, String stock, String sector, int quantity) {
        Optional<Holding> existing = holdingRepository.findByTraderIdAndStockForUpdate(traderId, stock);

        if (existing.isPresent()) {
            Holding holding = existing.get();
            holding.setQuantity(holding.getQuantity() + quantity);
            return holdingRepository.save(holding);
        } else {
            Holding holding = new Holding();
            holding.setTraderId(traderId);
            holding.setStock(stock);
            holding.setSector(sector);
            holding.setQuantity(quantity);
            return holdingRepository.save(holding);
        }
    }

    /**
     * Updates holding after an order fill.
     * Must be called within an existing transaction (MANDATORY propagation).
     * For BUY: increases holding quantity (creates if not exists).
     * For SELL: decreases holding quantity.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void updateHoldingForFill(String traderId, String stock, String sector, int quantity, Side side) {
        Optional<Holding> existing = holdingRepository.findByTraderIdAndStockForUpdate(traderId, stock);

        if (side == Side.BUY) {
            if (existing.isPresent()) {
                Holding holding = existing.get();
                holding.setQuantity(holding.getQuantity() + quantity);
                holdingRepository.save(holding);
            } else {
                Holding holding = new Holding();
                holding.setTraderId(traderId);
                holding.setStock(stock);
                holding.setSector(sector);
                holding.setQuantity(quantity);
                holdingRepository.save(holding);
            }
        } else {
            // SELL: holding must exist (validated during order placement)
            Holding holding = existing.get();
            holding.setQuantity(holding.getQuantity() - quantity);
            holdingRepository.save(holding);
        }
    }
}
