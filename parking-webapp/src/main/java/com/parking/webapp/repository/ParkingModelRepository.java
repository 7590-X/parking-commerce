package com.parking.webapp.repository;

import com.parking.webapp.model.ParkingModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParkingModelRepository extends JpaRepository<ParkingModel, Integer > {
    List<ParkingModel> findByDeviceId(String deviceId);
}
