package com.parking.webapp.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.parking.webapp.domain.exception.ParkingFullException;
import com.parking.webapp.domain.exception.ParkingNotFoundException;
import com.parking.webapp.domain.exception.TicketNotFoundException;
import com.parking.webapp.domain.tariff.TariffStrategy;
import com.parking.webapp.dto.ExitResult;
import com.parking.webapp.dto.ParkingDto;
import com.parking.webapp.dto.TelemetryDto;
import com.parking.webapp.enums.EntryRequestKernel;
import com.parking.webapp.enums.EntryResponseKernel;
import com.parking.webapp.events.TicketCreatedEvent;
import com.parking.webapp.model.ParkingModel;
import com.parking.webapp.model.TicketModel;
import com.parking.webapp.ports.output.HardwareBarrierPort;
import com.parking.webapp.repository.ParkingRepository;
import com.parking.webapp.repository.TicketRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementación desacoplada y transaccional de los servicios de gestión de parqueo.
 * Aplica principios SOLID:
 * - SRP: Delega el cálculo de tarifas a {@link TariffStrategy} y el hardware a {@link HardwareBarrierPort}.
 * - OCP: Nuevas tarifas o transportes de hardware se integran sin alterar esta clase.
 * - DIP: Depende de interfaces y puertos, no de implementaciones concretas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ParkingServiceImpl implements ParkingBrokerService, ParkingWebService {

    private static final int DEFAULT_PARKING_ID = 1;
    private static final String RK_RESPONSE_IN = "kernel.arduino.response.in";
    private static final String RK_RESPONSE_OUT = "kernel.arduino.response.out";

    // Repositorios de persistencia
    private final ParkingRepository parkingRepository;
    private final TicketRepository ticketRepository;

    // Puertos y estrategias desacopladas
    private final HardwareBarrierPort hardwarePort;
    private final TariffStrategy tariffStrategy;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Procesa la solicitud de entrada emitida por el microcontrolador.
     * Reserva el espacio de forma atómica en base de datos para prevenir condiciones de carrera.
     */
    @Override
    @Transactional
    public void verifyParkingSpace(String payload) {
        if (!payload.equals(EntryRequestKernel.CHECK_SPACE.name())) {
            log.warn("Payload [{}] no válido para chequeo de espacio", payload);
            return;
        }

        boolean spaceReserved = parkingRepository.incrementCapacityIfAvailable(DEFAULT_PARKING_ID, Instant.now()) > 0;
        if (spaceReserved) {
            TicketModel ticket = generateTicket();
            hardwarePort.sendMQResponse(RK_RESPONSE_IN, EntryResponseKernel.ALLOW.name());
            log.info("Acceso autorizado. Espacio reservado y ticket emitido: {}", ticket.getUuid());
        } else {
            hardwarePort.sendMQResponse(RK_RESPONSE_IN, EntryResponseKernel.DENY.name());
            log.warn("Acceso denegado: Parqueo sin disponibilidad de aforo.");
        }
    }

    /**
     * Genera un nuevo ticket de estacionamiento, lo persiste y emite el evento de dominio.
     */
    private TicketModel generateTicket() {
        ParkingModel parking = parkingRepository.findById(DEFAULT_PARKING_ID)
                .orElseThrow(() -> new ParkingNotFoundException(DEFAULT_PARKING_ID));

        TicketModel ticket = TicketModel.builder()
                .uuid(UUID.randomUUID().toString())
                .entryTime(Instant.now())
                .isPaid(false)
                .parking(parking)
                .build();

        TicketModel persisted = ticketRepository.saveAndFlush(ticket);
        log.info("Ticket persistido: {}", persisted.getUuid());

        // Emitir evento de dominio Spring
        eventPublisher.publishEvent(new TicketCreatedEvent(this, persisted));

        return persisted;
    }

    @Override
    public void checkOutQR(String payload) {
        if (payload.equals(EntryRequestKernel.CHECK_QR.name())) {
            log.info("Chequeo de QR recibido desde broker");
        } else {
            log.warn("Payload [{}] no válido para chequeo de ticket: {}", payload, payload);
        }
    }

    @Override
    public TelemetryDto obtainTelemetry() {
        ParkingModel parking = parkingRepository.findById(DEFAULT_PARKING_ID)
                .orElseThrow(() -> new ParkingNotFoundException(DEFAULT_PARKING_ID));

        return new TelemetryDto(parking.getMaxCapacity(), parking.getCurrentCapacity(), parking.getLastUpdated());
    }

    @Override
    public ParkingDto obtainParking() {
        ParkingModel parking = parkingRepository.findById(DEFAULT_PARKING_ID)
                .orElseThrow(() -> new ParkingNotFoundException(DEFAULT_PARKING_ID));

        return new ParkingDto(parking.getId(), parking.getName(), parking.getAddress());
    }

    @Override
    public ParkingModel getParkingInfo() {
        return parkingRepository.findById(DEFAULT_PARKING_ID).orElse(null);
    }

    @Override
    public Optional<TicketModel> findTicket(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return Optional.empty();
        }
        return ticketRepository.findByUuid(uuid.trim());
    }

    @Override
    public BigDecimal calculateCurrentFee(TicketModel ticket) {
        if (ticket == null) {
            return BigDecimal.ZERO;
        }
        if (ticket.isPaid() && ticket.getAmount() != null && ticket.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            return ticket.getAmount();
        }
        Instant entryTime = ticket.getEntryTime() != null ? ticket.getEntryTime() : Instant.now();
        return tariffStrategy.calculateFee(entryTime, Instant.now());
    }

    @Override
    @Transactional
    public TicketModel payTicket(String uuid) {
        TicketModel ticket = ticketRepository.findByUuid(uuid.trim())
                .orElseThrow(() -> new TicketNotFoundException(uuid));

        if (!ticket.isPaid()) {
            BigDecimal fee = tariffStrategy.calculateFee(ticket.getEntryTime(), Instant.now());

            ticket.setPaid(true);
            ticket.setPaymentTime(Instant.now());
            ticket.setAmount(fee);

            ticket = ticketRepository.saveAndFlush(ticket);
            log.info("Ticket {} pagado exitosamente. Monto cobrado: Q{}", ticket.getUuid(), fee);
        }
        return ticket;
    }

    @Override
    @Transactional
    public ExitResult processExit(String uuid) {
        Optional<TicketModel> optTicket = findTicket(uuid);
        if (optTicket.isEmpty()) {
            log.warn("Intento de salida con ticket inexistente: {}", uuid);
            return ExitResult.error("El ticket con código " + uuid + " no fue encontrado en el sistema.");
        }

        TicketModel ticket = optTicket.get();

        if (ticket.getOutTime() != null) {
            log.warn("Ticket {} ya registró salida previamente", uuid);
            return ExitResult.error("Este ticket ya registró su salida del parqueo anteriormente.");
        }

        if (!ticket.isPaid()) {
            log.warn("Ticket {} no ha sido pagado. Denegando salida.", uuid);
            hardwarePort.sendMQResponse(RK_RESPONSE_OUT, EntryResponseKernel.DENY.name());
            return ExitResult.error("Ticket PENDIENTE DE PAGO. Debe pagarse antes de salir.");
        }

        // Registrar salida
        ticket.setOutTime(Instant.now());
        ticketRepository.saveAndFlush(ticket);

        // Decrementar ocupación atómicamente
        parkingRepository.decrementCapacity(DEFAULT_PARKING_ID, Instant.now());

        // Apertura física de la talanquera
        hardwarePort.sendBarrierCommand("OPEN");
        hardwarePort.sendMQResponse(RK_RESPONSE_OUT, EntryResponseKernel.ALLOW.name());
        log.info("Salida autorizada para ticket {}. Talanquera aperturada.", uuid);

        return ExitResult.ok("¡Salida Autorizada! Talanquera abierta. Buen viaje.", ticket);
    }

    @Override
    @Transactional
    public TicketModel createManualTicket() {
        boolean spaceReserved = parkingRepository.incrementCapacityIfAvailable(DEFAULT_PARKING_ID, Instant.now()) > 0;
        if (!spaceReserved) {
            throw new ParkingFullException(DEFAULT_PARKING_ID);
        }

        return generateTicket();
    }
}
