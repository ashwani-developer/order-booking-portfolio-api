package com.trading.repository;

import com.trading.model.Holding;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    List<Holding> findByTraderId(String traderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM Holding h WHERE h.traderId = :traderId AND h.stock = :stock")
    Optional<Holding> findByTraderIdAndStockForUpdate(
        @Param("traderId") String traderId, @Param("stock") String stock);
}
