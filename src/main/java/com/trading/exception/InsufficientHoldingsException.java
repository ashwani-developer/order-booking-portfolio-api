package com.trading.exception;

public class InsufficientHoldingsException extends OrderValidationException {

    public InsufficientHoldingsException(String message) {
        super(message);
    }
}
