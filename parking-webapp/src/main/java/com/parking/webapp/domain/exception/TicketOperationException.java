package com.parking.webapp.domain.exception;

public class TicketOperationException extends RuntimeException {
    public TicketOperationException(String message) {
        super(message);
    }
}
