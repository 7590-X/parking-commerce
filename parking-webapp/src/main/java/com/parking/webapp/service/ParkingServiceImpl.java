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

    private void generateTicket() {
        ParkingModel parking = parkingRepository.findById(1).orElse(null);
        if (null == parking) {
            log.error("No se encontró un parqueo para la generación de ticket");
            return;
        }
        TicketModel ticket = TicketModel.builder()
                .uuid(UUID.randomUUID().toString())
                .entryTime(Instant.now())
                .isPayed(false)
                .parking(parking)
                .build();

        TicketModel persisted = ticketRepository.save(ticket);
        log.info("Ticket generado: {}", persisted.getUuid());
    }

    @Override
    public void checkOutQR(String payload) {
        if (payload.equals(EntryRequestKernel.CHECK_QR.name())) {
            log.info("Chequeo de QR recibido desde broker");
        } else {
            log.warn("Payload [{}] no válido para el proceso de chequeo de ticket", payload);
        }
    }

    @Override
    public TelemetryDto obtainTelemetry() {
        ParkingModel parking = parkingRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("No se encontró parqueo para lectura de telemetría"));

        return new TelemetryDto(parking.getMaxCapacity(), parking.getCurrentCapacity(), parking.getLastUpdated());
    }

    @Override
    public ParkingDto obtainParking() {
        ParkingModel parking = parkingRepository.findById(1)
                .orElseThrow(() -> new RuntimeException("No se encontró parqueo para obtener sus datos"));
        return new ParkingDto(parking.getId(), parking.getName(), parking.getAddress());
    }

    @Override
    public ParkingModel getParkingInfo() {
        return parkingRepository.findById(1).orElse(null);
    }

    @Override
    public Optional<TicketModel> findTicket(String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return Optional.empty();
        }
        return ticketRepository.findByUuid(uuid.trim());
    }

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

    @Override
    public TicketModel createManualTicket() {
        ParkingModel parking = parkingRepository.findById(1).orElse(null);
        if (parking == null) {
            throw new IllegalStateException("No existe parqueo ID 1 configurado");
        }
        TicketModel ticket = TicketModel.builder()
                .uuid(UUID.randomUUID().toString())
                .entryTime(Instant.now())
                .isPayed(false)
                .parking(parking)
                .build();

        validateParkingSpace();
        return ticketRepository.saveAndFlush(ticket);
    }
}
