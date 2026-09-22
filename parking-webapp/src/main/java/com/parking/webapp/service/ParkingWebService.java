package com.parking.webapp.service;

import java.util.Optional;

import com.parking.webapp.dto.ExitResult;
import com.parking.webapp.dto.ParkingDto;
import com.parking.webapp.dto.TelemetryDto;
import com.parking.webapp.model.ParkingModel;
import com.parking.webapp.model.TicketModel;

public interface ParkingWebService {

    TelemetryDto obtainTelemetry();

    ParkingDto obtainParking();

    ParkingModel getParkingInfo();

    Optional<TicketModel> findTicket(String uuid);

    TicketModel payTicket(String uuid);

    ExitResult processExit(String uuid);

    TicketModel createManualTicket();
}

