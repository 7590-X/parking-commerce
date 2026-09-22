package com.parking.webapp.domain.exception;

public class TicketNotFoundException extends RuntimeException {
    public TicketNotFoundException(String uuid) {
        super("No se encontró ningún ticket registrado con el código: " + uuid);
    }
}
