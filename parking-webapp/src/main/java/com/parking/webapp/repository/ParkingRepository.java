package com.parking.webapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.parking.webapp.model.ParkingModel;

public interface ParkingRepository extends JpaRepository<ParkingModel, Integer> {

}
