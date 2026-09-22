package com.parking.webapp.domain.exception;

public class ParkingFullException extends RuntimeException {
    public ParkingFullException(int id) {
        super("El parqueo ID " + id + " ha alcanzado su capacidad máxima permitida.");
    }
}
