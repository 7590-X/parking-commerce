package com.parking.webapp.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.parking.webapp.model.TicketModel;

public interface TicketRepository extends JpaRepository<TicketModel, Integer> {

    Optional<TicketModel> findByUuid(String uuid);
}
