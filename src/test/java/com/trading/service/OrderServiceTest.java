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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private HoldingRepository holdingRepository;

    @Mock
    private PortfolioService portfolioService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrder_rejectsPendingLimitReached() {
        List<Order> threeOrders = List.of(new Order(), new Order(), new Order());
        when(orderRepository.findPendingOrdersByTraderIdForUpdate("trader1")).thenReturn(threeOrders);

        assertThatThrownBy(() -> orderService.placeOrder("trader1", "AAPL", "TECH", 10, Side.BUY))
            .isInstanceOf(PendingOrderLimitException.class);
    }

    @Test
    void placeOrder_rejectsSellWithInsufficientHoldings() {
        when(orderRepository.findPendingOrdersByTraderIdForUpdate("trader1")).thenReturn(List.of());

        Holding holding = new Holding();
        holding.setQuantity(10);
        when(holdingRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL"))
            .thenReturn(Optional.of(holding));

        assertThatThrownBy(() -> orderService.placeOrder("trader1", "AAPL", "TECH", 50, Side.SELL))
            .isInstanceOf(InsufficientHoldingsException.class);
    }

    @Test
    void placeOrder_acceptsSellWithSufficientHoldings() {
        when(orderRepository.findPendingOrdersByTraderIdForUpdate("trader1")).thenReturn(List.of());

        Holding holding = new Holding();
        holding.setQuantity(100);
        when(holdingRepository.findByTraderIdAndStockForUpdate("trader1", "AAPL"))
            .thenReturn(Optional.of(holding));

        Order savedOrder = new Order();
        savedOrder.setState(OrderState.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = orderService.placeOrder("trader1", "AAPL", "TECH", 50, Side.SELL);

        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(OrderState.PENDING);
    }

    @Test
    void placeOrder_acceptsBuyOrder() {
        when(orderRepository.findPendingOrdersByTraderIdForUpdate("trader1")).thenReturn(List.of());

        Order savedOrder = new Order();
        savedOrder.setState(OrderState.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        Order result = orderService.placeOrder("trader1", "AAPL", "TECH", 10, Side.BUY);

        assertThat(result).isNotNull();
        assertThat(result.getState()).isEqualTo(OrderState.PENDING);
    }

    @Test
    void fillOrder_throwsNotFoundForNonExistentOrder() {
        when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.fillOrder(99L))
            .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void fillOrder_throwsConflictForFilledOrder() {
        Order order = new Order();
        order.setState(OrderState.FILLED);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.fillOrder(1L))
            .isInstanceOf(OrderStateConflictException.class);
    }

    @Test
    void fillOrder_throwsConflictForCancelledOrder() {
        Order order = new Order();
        order.setState(OrderState.CANCELLED);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.fillOrder(1L))
            .isInstanceOf(OrderStateConflictException.class);
    }

    @Test
    void fillOrder_transitionsToFilled() {
        Order order = new Order();
        order.setId(1L);
        order.setTraderId("trader1");
        order.setStock("AAPL");
        order.setSector("TECH");
        order.setQuantity(10);
        order.setSide(Side.BUY);
        order.setState(OrderState.PENDING);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.fillOrder(1L);

        assertThat(result.getState()).isEqualTo(OrderState.FILLED);
        verify(portfolioService).updateHoldingForFill("trader1", "AAPL", "TECH", 10, Side.BUY);
    }

    @Test
    void cancelOrder_throwsNotFoundForNonExistentOrder() {
        when(orderRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(99L))
            .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancelOrder_throwsConflictForFilledOrder() {
        Order order = new Order();
        order.setState(OrderState.FILLED);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
            .isInstanceOf(OrderStateConflictException.class)
            .hasMessageContaining("FILLED");
    }

    @Test
    void cancelOrder_throwsConflictForCancelledOrder() {
        Order order = new Order();
        order.setState(OrderState.CANCELLED);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
            .isInstanceOf(OrderStateConflictException.class)
            .hasMessageContaining("CANCELLED");
    }

    @Test
    void cancelOrder_transitionsToCancelled() {
        Order order = new Order();
        order.setId(1L);
        order.setTraderId("trader1");
        order.setState(OrderState.PENDING);
        when(orderRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.cancelOrder(1L);

        assertThat(result.getState()).isEqualTo(OrderState.CANCELLED);
    }
}
