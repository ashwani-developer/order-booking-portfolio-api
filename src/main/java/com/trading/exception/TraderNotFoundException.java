package com.trading.exception;

public class TraderNotFoundException extends ResourceNotFoundException {

    public TraderNotFoundException(String message) {
        super(message);
    }
}
