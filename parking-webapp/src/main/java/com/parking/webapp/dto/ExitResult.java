package com.parking.webapp.dto;

import com.parking.webapp.model.TicketModel;

public record ExitResult(
        boolean success,
        String message,
        TicketModel ticket
) {
    public static ExitResult ok(String message, TicketModel ticket) {
        return new ExitResult(true, message, ticket);
    }

    public static ExitResult error(String message) {
        return new ExitResult(false, message, null);
    }
}
