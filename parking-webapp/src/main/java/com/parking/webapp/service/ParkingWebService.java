package com.parking.webapp.service;

import com.parking.webapp.dto.ParkingDto;
import com.parking.webapp.dto.TelemetryDto;

public interface ParkingWebService {

    TelemetryDto obtainTelemetry();

    ParkingDto obtainParking();

}
