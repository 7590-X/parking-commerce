package com.parking.webapp.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.parking.webapp.broker.ArduinoKernelProducer;
import com.parking.webapp.dto.ExitResult;
import com.parking.webapp.dto.ParkingDto;
import com.parking.webapp.dto.TelemetryDto;
import com.parking.webapp.enums.EntryRequestKernel;
import com.parking.webapp.enums.EntryResponseKernel;
import com.parking.webapp.model.ParkingModel;
import com.parking.webapp.model.TicketModel;
import com.parking.webapp.repository.ParkingRepository;
import com.parking.webapp.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación de los servicios de gestión del parqueo {@link ParkingBrokerService} y {@link ParkingWebService}.
 * Coordina la persistencia de datos (tickets y ocupación) junto con la comunicación asíncrona
 * hacia el hardware controlador (Arduino) mediante RabbitMQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingBrokerService, ParkingWebService {

    // Repositories
    private final ParkingRepository parkingRepository;
    private final TicketRepository ticketRepository;

    // Broker Producers
    private final ArduinoKernelProducer producer;

    // Broker Routing Keys
    private static final String RK_RESPONSE_IN = "kernel.arduino.response.in";
    private static final String RK_RESPONSE_OUT = "kernel.arduino.response.out";

    /**
     * Verifica la disponibilidad de espacio ante una solicitud recibida desde el broker.
     * Si el espacio es suficiente, genera un ticket e instruye al controlador permitir el paso (ALLOW);
     * de lo contrario, envía una respuesta de denegación (DENY).
     *
     * @param payload Comando recibido desde el broker (se espera {@link EntryRequestKernel#CHECK_SPACE}).
     */
    @Override
    public void verifyParkingSpace(String payload) {
        if (!payload.equals(EntryRequestKernel.CHECK_SPACE.name())) {
            log.warn("Payload [{}] no válido para el proceso de chequeo de parqueo", payload);
            return;
        }
        boolean isValid = validateParkingSpace();
        if (isValid) {
            generateTicket();
            producer.sendMQResponse(RK_RESPONSE_IN, EntryResponseKernel.ALLOW.name());
        } else {
            producer.sendMQResponse(RK_RESPONSE_IN, EntryResponseKernel.DENY.name());
        }
    }

    /**
     * Comprueba si el parqueo cuenta con espacios disponibles respecto a su capacidad máxima.
     * Si hay disponibilidad, incrementa en uno la capacidad actual y persiste los cambios en la base de datos.
     *
     * @return {@code true} si se reservó exitosamente un espacio; {@code false} si el parqueo está lleno o no existe.
     */
    private boolean validateParkingSpace() {
        ParkingModel parking = parkingRepository.findById(1).orElse(null);
        if (null == parking) {
            log.error("No se encontró un parqueo asociado");
            return false;
        }
        short current = parking.getCurrentCapacity();
        short max = parking.getMaxCapacity();
        if (current < max) {
            parking.setCurrentCapacity((short) (current + 1));
            parking.setLastUpdated(Instant.now());
            parkingRepository.saveAndFlush(parking);
            return true;
        }
        return false;
    }

    /**
     * Genera y persiste un nuevo ticket de estacionamiento con un identificador único (UUID),
     * fecha/hora de entrada actual y estado inicial no pagado.
     * Emite además el evento para visualización e impresión en el frontend.
     */
    private TicketModel generateTicket() {
        ParkingModel parking = parkingRepository.findById(1).orElse(null);
        if (null == parking) {
            log.error("No se encontró un parqueo para la generación de ticket");
            return null;
        }
        TicketModel ticket = TicketModel.builder()
                .uuid(UUID.randomUUID().toString())
                .entryTime(Instant.now())
                .isPayed(false)
                .parking(parking)
                .build();

        TicketModel persisted = ticketRepository.saveAndFlush(ticket);
        log.info("Ticket generado: {}", persisted.getUuid());

        // Notificar al frontend para mostrar diálogo de impresión
        TicketBroadcaster.broadcast(persisted);

        return persisted;
    }

    /**
     * Registra en el log la recepción de una solicitud de validación de código QR desde el broker.
     *
     * @param payload Comando recibido (se espera {@link EntryRequestKernel#CHECK_QR}).
     */
    @Override
    public void checkOutQR(String payload) {
        if (payload.equals(EntryRequestKernel.CHECK_QR.name())) {
            log.info("Chequeo de QR recibido desde broker");
        } else {
            log.warn("Payload [{}] no válido para el proceso de chequeo de ticket", payload);
        }
    }

    /**
     * Consulta la telemetría actual del parqueo (capacidad máxima, capacidad ocupada y marca de tiempo).
     *
     * @return DTO {@link TelemetryDto} con los valores actuales.
     * @throws RuntimeException Si no se encuentra el registro del parqueo principal en la base de datos.
     */
    @Override
    public TelemetryDto obtainTelemetry() {
        ParkingModel parking = parkingRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("No se encontró parqueo para lectura de telemetría"));

        return new TelemetryDto(parking.getMaxCapacity(), parking.getCurrentCapacity(), parking.getLastUpdated());
    }

    /**
     * Consulta y mapea los datos descriptivos del parqueo (ID, nombre y dirección).
     *
     * @return DTO {@link ParkingDto} con la información del parqueo.
     * @throws RuntimeException Si no se encuentra el registro del parqueo principal en la base de datos.
     */
    @Override
    public ParkingDto obtainParking() {
        ParkingModel parking = parkingRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("No se encontró parqueo para obtener sus datos"));
        return new ParkingDto(parking.getId(), parking.getName(), parking.getAddress());
    }

    /**
     * Recupera la entidad completa del parqueo principal.
     *
     * @return Objeto {@link ParkingModel} con la información del parqueo, o {@code null} si no existe.
     */
    @Override
    public ParkingModel getParkingInfo() {
        return parkingRepository.findById(1).orElse(null);
    }

    /**
     * Busca un ticket por su identificador UUID en la base de datos.
     *
     * @param uuid Cadena con el identificador único del ticket.
     * @return Un {@link Optional} con el {@link TicketModel} si existe, o vacío si el UUID es nulo, en blanco o no existe.
     */
    @Override
    public Optional<TicketModel> findTicket(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return Optional.empty();
        }
        return ticketRepository.findByUuid(uuid.trim());
    }

    /**
     * Realiza el cobro de un ticket registrando su estado pagado, fecha/hora de pago
     * y calculando la tarifa según el tiempo transcurrido (Tarifa base Q10.00 + Q5.00 por hora extra).
     *
     * @param uuid Identificador único del ticket a procesar.
     * @return {@link TicketModel} actualizado con el pago registrado.
     * @throws IllegalArgumentException Si el ticket no es encontrado con el código provisto.
     */
    @Override
    public TicketModel payTicket(String uuid) {
        TicketModel ticket = ticketRepository.findByUuid(uuid.trim())
                .orElseThrow(() -> new IllegalArgumentException("Ticket no encontrado con código: " + uuid));

        if (!ticket.isPayed()) {
            ticket.setPayed(true);
            ticket.setPaymentTime(Instant.now());

            // Cálculo de tarifa: Base Q10.00 + Q5.00 por hora transcurrida adicional
            Instant entry = ticket.getEntryTime() != null ? ticket.getEntryTime() : Instant.now();
            long minutes = Math.max(1, Duration.between(entry, Instant.now()).toMinutes());
            long hours = (long) Math.ceil(minutes / 60.0);
            float calculatedAmount = 10.0f + Math.max(0, hours - 1) * 5.0f;

            ticket.setAmount(calculatedAmount);
            ticket = ticketRepository.saveAndFlush(ticket);
            log.info("Ticket {} pagado exitosamente con monto Q{}", ticket.getUuid(), calculatedAmount);
        }
        return ticket;
    }

    /**
     * Procesa la solicitud de salida de un vehículo mediante su ticket:
     * valida existencia, estado de pago y que no haya salido con anterioridad.
     * Si es autorizado, registra la salida, decrementa la ocupación del parqueo y
     * envía la orden de apertura de barrera al microcontrolador a través del broker.
     *
     * @param uuid Identificador único del ticket que solicita la salida.
     * @return {@link ExitResult} indicando si la salida fue autorizada o la causa del rechazo.
     */
    @Override
    public ExitResult processExit(String uuid) {
        Optional<TicketModel> optTicket = findTicket(uuid);
        if (optTicket.isEmpty()) {
            log.warn("Intento de salida con ticket inexistente: {}", uuid);
            return ExitResult.error("El ticket con código " + uuid + " no fue encontrado en el sistema.");
        }

        TicketModel ticket = optTicket.get();

        if (ticket.getOutTime() != null) {
            log.warn("Ticket {} ya registró su salida previamente", uuid);
            return ExitResult.error("Este ticket ya registró su salida del parqueo anteriormente.");
        }

        if (!ticket.isPayed()) {
            log.warn("Ticket {} no ha sido pagado. Denegando salida.", uuid);
            producer.sendMQResponse(RK_RESPONSE_OUT, EntryResponseKernel.DENY.name());
            return ExitResult.error("Ticket PENDIENTE DE PAGO. Debe pagarse antes de salir.");
        }

        // Salida Autorizada
        ticket.setOutTime(Instant.now());
        ticketRepository.saveAndFlush(ticket);

        // Disminuir ocupación del parqueo
        ParkingModel parking = parkingRepository.findById(1).orElse(null);
        if (parking != null) {
            short current = parking.getCurrentCapacity();
            if (current > 0) {
                parking.setCurrentCapacity((short) (current - 1));
            }
            parking.setLastUpdated(Instant.now());
            parkingRepository.saveAndFlush(parking);
        }

        // Enviar orden de apertura de barrera
        producer.sendBarrierCommand("OPEN");
        producer.sendMQResponse(RK_RESPONSE_OUT, EntryResponseKernel.ALLOW.name());
        log.info("Salida autorizada para ticket {}. Barrera aperturada.", uuid);

        return ExitResult.ok("¡Salida Autorizada! Talanquera abierta. Buen viaje.", ticket);
    }

    /**
     * Crea un ticket de forma manual (por ejemplo, desde la interfaz administrativa web),
     * actualiza la ocupación del parqueo y persiste el nuevo ticket.
     *
     * @return {@link TicketModel} recién generado y persistido.
     * @throws IllegalStateException Si no existe el registro del parqueo principal.
     */
    @Override
    public TicketModel createManualTicket() {
        ParkingModel parking = parkingRepository.findById(1).orElse(null);
        if (parking == null) {
            throw new IllegalStateException("No existe parqueo ID 1 configurado");
        }
        boolean hasSpace = validateParkingSpace();
        if (!hasSpace) {
            throw new IllegalStateException("El parqueo ha alcanzado su capacidad máxima");
        }
        TicketModel ticket = TicketModel.builder()
                .uuid(UUID.randomUUID().toString())
                .entryTime(Instant.now())
                .isPayed(false)
                .parking(parking)
                .build();

        TicketModel persisted = ticketRepository.saveAndFlush(ticket);
        log.info("Ticket manual generado: {}", persisted.getUuid());

        // Notificar al frontend para mostrar diálogo de impresión
        TicketBroadcaster.broadcast(persisted);

        return persisted;
    }
}
