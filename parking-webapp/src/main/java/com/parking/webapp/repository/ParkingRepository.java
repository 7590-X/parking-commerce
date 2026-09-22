package com.parking.webapp.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.parking.webapp.model.ParkingModel;

public interface ParkingRepository extends JpaRepository<ParkingModel, Integer> {

    /**
     * Incrementa atómicamente la capacidad actual si existe cupo disponible respecto al máximo.
     *
     * @param id  Identificador del parqueo.
     * @param now Marca de tiempo de la actualización.
     * @return 1 si se reservó exitosamente un espacio, 0 si el parqueo está lleno o no existe.
     */
    @Modifying
    @Query("UPDATE ParkingModel p SET p.currentCapacity = (p.currentCapacity + 1), p.lastUpdated = :now WHERE p.id = :id AND p.currentCapacity < p.maxCapacity")
    int incrementCapacityIfAvailable(@Param("id") int id, @Param("now") Instant now);

    /**
     * Decrementa atómicamente la capacidad actual si es mayor a cero.
     *
     * @param id  Identificador del parqueo.
     * @param now Marca de tiempo de la actualización.
     * @return 1 si se decrementó exitosamente, 0 en caso contrario.
     */
    @Modifying
    @Query("UPDATE ParkingModel p SET p.currentCapacity = (p.currentCapacity - 1), p.lastUpdated = :now WHERE p.id = :id AND p.currentCapacity > 0")
    int decrementCapacity(@Param("id") int id, @Param("now") Instant now);
}

