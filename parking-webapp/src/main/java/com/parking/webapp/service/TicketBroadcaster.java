package com.parking.webapp.service;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.parking.webapp.model.TicketModel;
import com.vaadin.flow.shared.Registration;

import lombok.extern.slf4j.Slf4j;

/**
 * Transmisor de eventos compatible hacia atrás para notificaciones de tickets.
 * @deprecated Se recomienda inyectar {@link com.parking.webapp.events.TicketUiBroadcaster} como bean de Spring.
 */
@Slf4j
@Deprecated
public final class TicketBroadcaster {

    private static final CopyOnWriteArrayList<Consumer<TicketModel>> LISTENERS = new CopyOnWriteArrayList<>();

    private TicketBroadcaster() {
    }

    /**
     * Registra un nuevo escuchador para recibir tickets generados.
     */
    public static Registration register(Consumer<TicketModel> listener) {
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    /**
     * Emite un ticket a todos los escuchadores activos.
     */
    public static void broadcast(TicketModel ticket) {
        if (ticket == null) {
            return;
        }
        for (Consumer<TicketModel> listener : LISTENERS) {
            try {
                listener.accept(ticket);
            } catch (Exception e) {
                log.error("Error al notificar ticket a escuchador: {}", e.getMessage());
            }
        }
    }
}
