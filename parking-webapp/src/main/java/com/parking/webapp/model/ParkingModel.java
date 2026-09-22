package com.parking.webapp.model;

import java.time.Instant;

import com.vaadin.copilot.shaded.classgraph.nonapi.json.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "tb_parking")
public class ParkingModel {

    @Id
    @Column(name = "id")
    private int id;

    @Column(name = "name")
    private String name;

    @Column(name = "address")
    private String address;

    @Column(name = "max_capacity")
    private short maxCapacity;

    @Column(name = "current_capacity")
    private short currentCapacity;

    @Column(name = "last_updated")
    private Instant lastUpdated;
}
