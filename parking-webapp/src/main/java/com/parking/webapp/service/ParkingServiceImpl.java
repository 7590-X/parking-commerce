package com.parking.webapp.service;

import java.time.Instant;
import org.springframework.stereotype.Service;
import com.parking.webapp.broker.ArduinoKernelProducer;
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
        if (payload.equals(EntryRequestKernel.CHECK_SPACE.name())) {
            log.warn("Payload [{}] no válido para el procesdo de chequeo de parqueo", payload);
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
            parkingRepository.saveAndFlush(parking);
            return true;
        }
        return false;
    }

    private void generateTicket() {
        ParkingModel parking = parkingRepository.findById(1).orElse(null);
        if (null == parking) {
            log.error("No se encontró un parquo para la generación de ticket");
            return;
        }
        TicketModel ticket = TicketModel.builder()
                .entryTime(Instant.now())
                .isPayed(false)
                .parking(parking)
                .build();

        TicketModel persisted = ticketRepository.save(ticket);
        // Enviar evento para frontend para
    }

    @Override
    public void checkOutQR(String payload) {
        if (payload.equals(EntryRequestKernel.CHECK_QR.name())) {

            // enviar evento para chequeo de ticket
        } else {
            log.warn("Payload [{}] no válido para el procesdo de chequeo de ticket", payload);
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
}
