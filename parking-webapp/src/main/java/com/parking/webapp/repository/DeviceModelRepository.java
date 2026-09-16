package com.parking.webapp.repository;

import com.parking.webapp.model.DeviceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceModelRepository extends JpaRepository<DeviceModel, String> {

}
