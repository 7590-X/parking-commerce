package com.parking.webapp.domain.exception;

public class ParkingNotFoundException extends RuntimeException {
    public ParkingNotFoundException(int id) {
        super("No se encontró el parqueo con identificador ID: " + id);
    }
}
