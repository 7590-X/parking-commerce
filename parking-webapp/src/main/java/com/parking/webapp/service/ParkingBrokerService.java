package com.parking.webapp.service;

public interface ParkingBrokerService {

    void verifyParkingSpace(String payload);

    void checkOutQR(String payload);
}
