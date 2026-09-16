package com.parking.webapp.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tbl_parking")
public class ParkingModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public int id;

    @Column(name = "block", nullable = false)
    private String block;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "pin_in", nullable = false)
    private int pinIn;

    @Column(name = "pin_out", nullable = false)
    private int pinOut;

    @Column(name = "pin_led_green", nullable = false)
    private int pinLedGreen;

    @Column(name = "pin_led_red", nullable = false)
    private int pinLedRed;

    @Column(name = "device_id", nullable = false)
    private String deviceId;
}
