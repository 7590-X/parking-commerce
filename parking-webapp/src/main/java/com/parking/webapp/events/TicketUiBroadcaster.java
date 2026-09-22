package com.parking.webapp.events;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.parking.webapp.model.TicketModel;
import com.vaadin.flow.shared.Registration;

import lombok.extern.slf4j.Slf4j;

/**
 * Componente gestionado por Spring que escucha eventos {@link TicketCreatedEvent}
 * y distribuye las notificaciones a las interfaces Vaadin registradas.
 */
@Slf4j
@Component
public class TicketUiBroadcaster {

    private final CopyOnWriteArrayList<Consumer<TicketModel>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Registra un nuevo escuchador de UI para eventos de tickets.
     *
     * @param listener Consumidor a ejecutar.
     * @return {@link Registration} para cancelar el registro al desmontar la vista.
     */
    public Registration register(Consumer<TicketModel> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    /**
     * Manejador de eventos de dominio de Spring.
     *
     * @param event Evento con el ticket recién generado.
     */
    @EventListener
    public void onTicketCreated(TicketCreatedEvent event) {
        TicketModel ticket = event.getTicket();
        if (ticket == null) {
            return;
        }
        log.info("Notificando ticket [{}] a {} escuchadores activos de UI", ticket.getUuid(), listeners.size());
        for (Consumer<TicketModel> listener : listeners) {
            try {
                listener.accept(ticket);
            } catch (Exception e) {
                log.error("Error al despachar ticket a vista: {}", e.getMessage());
            }
        }
    }
}
