package com.trading.service;

import com.trading.dto.PortfolioResponse;
import com.trading.model.Holding;
import com.trading.repository.HoldingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    @Mock
    private HoldingRepository holdingRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    @Test
    void getPortfolio_returnsEmptyMapsForNoHoldings() {
        when(holdingRepository.findByTraderId("trader1")).thenReturn(List.of());

        PortfolioResponse response = portfolioService.getPortfolio("trader1");

        assertThat(response.positions()).isEmpty();
        assertThat(response.sectorBreakdown()).isEmpty();
    }

    @Test
    void getPortfolio_buildsSectorBreakdownCorrectly() {
        Holding aapl = createHolding("AAPL", "TECH", 100);
        Holding tsla = createHolding("TSLA", "TECH", 50);
        Holding jpm = createHolding("JPM", "FINANCE", 200);

        when(holdingRepository.findByTraderId("trader1")).thenReturn(List.of(aapl, tsla, jpm));

        PortfolioResponse response = portfolioService.getPortfolio("trader1");

        assertThat(response.sectorBreakdown()).containsEntry("TECH", 150);
        assertThat(response.sectorBreakdown()).containsEntry("FINANCE", 200);
    }

    @Test
    void addHolding_createsNewHoldingWhenNoneExists() {
        when(holdingRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL"))
            .thenReturn(Optional.empty());

        Holding savedHolding = createHolding("AAPL", "TECH", 50);
        when(holdingRepository.save(any(Holding.class))).thenReturn(savedHolding);

        Holding result = portfolioService.addHolding("trader1", "AAPL", "TECH", 50);

        assertThat(result.getStock()).isEqualTo("AAPL");
        assertThat(result.getQuantity()).isEqualTo(50);
    }

    @Test
    void addHolding_incrementsExistingHolding() {
        Holding existing = createHolding("AAPL", "TECH", 100);
        when(holdingRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL"))
            .thenReturn(Optional.of(existing));
        when(holdingRepository.save(any(Holding.class))).thenAnswer(inv -> inv.getArgument(0));

        Holding result = portfolioService.addHolding("trader1", "AAPL", "TECH", 50);

        assertThat(result.getQuantity()).isEqualTo(150);
    }

    private Holding createHolding(String stock, String sector, int quantity) {
        Holding holding = new Holding();
        holding.setTraderId("trader1");
        holding.setStock(stock);
        holding.setSector(sector);
        holding.setQuantity(quantity);
        return holding;
    }
}
