package com.trading.service;

import com.trading.exception.InsufficientHoldingsException;
import com.trading.exception.OrderNotFoundException;
import com.trading.exception.OrderStateConflictException;
import com.trading.exception.PendingOrderLimitException;
import com.trading.model.Holding;
import com.trading.model.Order;
import com.trading.model.OrderState;
import com.trading.model.Side;
import com.trading.repository.HoldingRepository;
import com.trading.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final HoldingRepository holdingRepository;
    private final PortfolioService portfolioService;

    public OrderService(OrderRepository orderRepository,
                        HoldingRepository holdingRepository,
                        PortfolioService portfolioService) {
        this.orderRepository = orderRepository;
        this.holdingRepository = holdingRepository;
        this.portfolioService = portfolioService;
    }

    @Transactional
    public Order placeOrder(String traderId, String stock, String sector, int quantity, Side side) {
        // Acquire pessimistic lock on trader's pending orders for count check
        List<Order> pendingOrders = orderRepository.findPendingOrdersByTraderIdForUpdate(traderId);

        if (pendingOrders.size() >= 3) {
            throw new PendingOrderLimitException("Trader " + traderId + " already has 3 pending orders");
        }

        // For SELL orders: validate trader holds sufficient quantity
        if (side == Side.SELL) {
            Optional<Holding> holdingOpt = holdingRepository.findByTraderIdAndStockForUpdate(traderId, stock);
            int currentQty = holdingOpt.map(Holding::getQuantity).orElse(0);
            if (currentQty < quantity) {
                throw new InsufficientHoldingsException(
                    "Insufficient holdings: have " + currentQty + ", need " + quantity + " of " + stock);
            }
        }

        // Create Order entity with state PENDING
        Order order = new Order();
        order.setTraderId(traderId);
        order.setStock(stock);
        order.setSector(sector);
        order.setQuantity(quantity);
        order.setSide(side);
        order.setState(OrderState.PENDING);

        return orderRepository.save(order);
    }

    @Transactional
    public Order fillOrder(Long orderId) {
        // Acquire pessimistic lock on order
        Optional<Order> orderOpt = orderRepository.findByIdForUpdate(orderId);

        Order order = orderOpt.orElseThrow(
            () -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getState() != OrderState.PENDING) {
            throw new OrderStateConflictException("Cannot fill order in " + order.getState() + " state");
        }

        // Transition state to FILLED
        order.setState(OrderState.FILLED);

        // Update portfolio holdings
        portfolioService.updateHoldingForFill(
            order.getTraderId(), order.getStock(), order.getSector(),
            order.getQuantity(), order.getSide());

        log.info("Order state transition: orderId={}, traderId={}, PENDING → FILLED",
            order.getId(), order.getTraderId());

        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long orderId) {
        // Acquire pessimistic lock on order
        Optional<Order> orderOpt = orderRepository.findByIdForUpdate(orderId);

        Order order = orderOpt.orElseThrow(
            () -> new OrderNotFoundException("Order not found: " + orderId));

        if (order.getState() != OrderState.PENDING) {
            throw new OrderStateConflictException("Cannot cancel order in " + order.getState() + " state");
        }

        // Transition state to CANCELLED
        order.setState(OrderState.CANCELLED);

        log.info("Order state transition: orderId={}, traderId={}, PENDING → CANCELLED",
            order.getId(), order.getTraderId());

        return orderRepository.save(order);
    }
}
