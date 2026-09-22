package com.parking.webapp.conf;

import java.time.Instant;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.parking.webapp.model.ParkingModel;
import com.parking.webapp.repository.ParkingRepository;
import com.parking.webapp.repository.TicketRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(ParkingRepository parkingRepository, TicketRepository ticketRepository) {
        return args -> {
            // Inicializar parqueo principal si no existe
            ParkingModel parking = parkingRepository.findById(1).orElse(null);
            if (parking == null) {
                parking = ParkingModel.builder()
                        .id(1)
                        .name("Estacionamiento Campus Central")
                        .address("Edificio T-1, Nivel 1")
                        .maxCapacity((short) 40)
                        .currentCapacity((short) 0)
                        .lastUpdated(Instant.now())
                        .build();
                parking = parkingRepository.save(parking);
                log.info("Parqueo inicializado con éxito: ID={}", parking.getId());
            }
        };
    }
}
