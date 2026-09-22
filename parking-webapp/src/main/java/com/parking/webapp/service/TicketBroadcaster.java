package com.parking.webapp.service;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.parking.webapp.model.TicketModel;
import com.vaadin.flow.shared.Registration;

import lombok.extern.slf4j.Slf4j;

/**
 * Transmisor de eventos para notificar a los clientes web conectados cuando se genera un nuevo ticket,
 * permitiendo abrir automáticamente el diálogo de impresión en tiempo real.
 */
@Slf4j
public final class TicketBroadcaster {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final CopyOnWriteArrayList<Consumer<TicketModel>> LISTENERS = new CopyOnWriteArrayList<>();

    private TicketBroadcaster() {
    }

    /**
     * Registra un nuevo escuchador para recibir tickets generados.
     *
     * @param listener Consumidor que procesará el ticket generado.
     * @return {@link Registration} para cancelar el registro al desmontar la vista.
     */
    public static Registration register(Consumer<TicketModel> listener) {
        LISTENERS.add(listener);
        return () -> LISTENERS.remove(listener);
    }

    /**
     * Emite un ticket a todos los escuchadores activos.
     *
     * @param ticket Instancia del ticket recién creado.
     */
    public static void broadcast(TicketModel ticket) {
        if (ticket == null) {
            return;
        }
        log.info("Emitiendo evento de ticket generado a [{}] escuchadores: {}", LISTENERS.size(), ticket.getUuid());
        for (Consumer<TicketModel> listener : LISTENERS) {
            EXECUTOR.execute(() -> {
                try {
                    listener.accept(ticket);
                } catch (Exception e) {
                    log.error("Error al notificar ticket a escuchador: {}", e.getMessage());
                }
            });
        }
    }
}
