package com.trading.controller;

import com.trading.dto.ErrorResponse;
import com.trading.exception.OrderNotFoundException;
import com.trading.exception.OrderStateConflictException;
import com.trading.exception.PendingOrderLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleOrderValidation_returns400() {
        PendingOrderLimitException ex = new PendingOrderLimitException("Too many pending orders");

        ResponseEntity<ErrorResponse> response = handler.handleOrderValidation(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).contains("Too many pending orders");
    }

    @Test
    void handleNotFound_returns404() {
        OrderNotFoundException ex = new OrderNotFoundException("Order not found: 99");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).contains("Order not found: 99");
    }

    @Test
    void handleConflict_returns409() {
        OrderStateConflictException ex = new OrderStateConflictException("Cannot fill order in FILLED state");

        ResponseEntity<ErrorResponse> response = handler.handleConflict(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).contains("Cannot fill order in FILLED state");
    }

    @Test
    void handleUnexpected_returns500() {
        RuntimeException ex = new RuntimeException("Something went wrong");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).contains("Internal server error");
    }
}
