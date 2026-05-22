package com.trading.exception;

public class PendingOrderLimitException extends OrderValidationException {

    public PendingOrderLimitException(String message) {
        super(message);
    }
}
