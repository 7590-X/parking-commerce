package com.parking.webapp.events;

import org.springframework.context.ApplicationEvent;

import com.parking.webapp.model.TicketModel;

import lombok.Getter;

/**
 * Evento de dominio de Spring publicado cuando se genera un nuevo ticket de estacionamiento.
 */
@Getter
public class TicketCreatedEvent extends ApplicationEvent {

    private final TicketModel ticket;

    public TicketCreatedEvent(Object source, TicketModel ticket) {
        super(source);
        this.ticket = ticket;
    }
}
