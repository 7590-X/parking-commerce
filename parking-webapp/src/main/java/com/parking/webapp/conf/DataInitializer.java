package com.parking.webapp.conf;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.parking.webapp.model.ParkingModel;
import com.parking.webapp.model.TicketModel;
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
                        .maxCapacity((short) 0)
                        .currentCapacity((short) 0)
                        .lastUpdated(Instant.now())
                        .build();
                parking = parkingRepository.save(parking);
                log.info("Parqueo inicializado con éxito: ID={}", parking.getId());
            }

            // // Crear tickets de prueba si no existen
            // if (ticketRepository.count() == 0) {
            // // Ticket 1: Pendiente de pago (ingresó hace 1 hora)
            // TicketModel pendingTicket = TicketModel.builder()
            // .uuid("DEMO-PENDING-001")
            // .entryTime(Instant.now().minus(65, ChronoUnit.MINUTES))
            // .isPayed(false)
            // .parking(parking)
            // .build();
            // ticketRepository.save(pendingTicket);

            // // Ticket 2: Ya pagado (listo para probar salida)
            // TicketModel paidTicket = TicketModel.builder()
            // .uuid("DEMO-PAID-002")
            // .entryTime(Instant.now().minus(45, ChronoUnit.MINUTES))
            // .paymentTime(Instant.now().minus(5, ChronoUnit.MINUTES))
            // .isPayed(true)
            // .amount(10.0f)
            // .parking(parking)
            // .build();
            // ticketRepository.save(paidTicket);

            // // Ticket 3: Generado aleatorio
            // TicketModel randomTicket = TicketModel.builder()
            // .uuid(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
            // .entryTime(Instant.now().minus(15, ChronoUnit.MINUTES))
            // .isPayed(false)
            // .parking(parking)
            // .build();
            // ticketRepository.save(randomTicket);

            // log.info("Tickets de demostración generados: DEMO-PENDING-001, DEMO-PAID-002,
            // {}", randomTicket.getUuid());
            // }
        };
    }
}
