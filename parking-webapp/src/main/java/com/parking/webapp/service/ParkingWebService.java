package com.parking.webapp.service;

import java.util.Optional;

import com.parking.webapp.dto.ExitResult;
import com.parking.webapp.dto.ParkingDto;
import com.parking.webapp.dto.TelemetryDto;
import com.parking.webapp.model.ParkingModel;
import com.parking.webapp.model.TicketModel;

/**
 * Interfaz que define las operaciones de servicio web para la gestión del parqueo,
 * incluyendo consultas de telemetría, gestión y cobro de tickets y control de salidas.
 */
public interface ParkingWebService {

    /**
     * Obtiene la telemetría actual del estacionamiento (capacidad máxima, ocupación actual y última actualización).
     *
     * @return DTO {@link TelemetryDto} con los datos telemétricos del parqueo.
     */
    TelemetryDto obtainTelemetry();

    /**
     * Obtiene la información básica del parqueo (identificador, nombre y dirección).
     *
     * @return DTO {@link ParkingDto} con la información representativa del parqueo.
     */
    ParkingDto obtainParking();

    /**
     * Obtiene la entidad del parqueo principal con todos sus atributos actuales.
     *
     * @return Entidad {@link ParkingModel} o {@code null} si no existe registro.
     */
    ParkingModel getParkingInfo();

    /**
     * Busca un ticket en el sistema a partir de su código único UUID.
     *
     * @param uuid Identificador único del ticket a consultar.
     * @return Un {@link Optional} que contiene el {@link TicketModel} si fue encontrado, o vacío en caso contrario.
     */
    Optional<TicketModel> findTicket(String uuid);

    /**
     * Registra el pago de un ticket calculando automáticamente el monto total según
     * el tiempo transcurrido desde su ingreso (tarifa base + horas adicionales).
     *
     * @param uuid Identificador único del ticket a cobrar.
     * @return El {@link TicketModel} actualizado con el estado pagado y el monto registrado.
     * @throws IllegalArgumentException Si el ticket no es encontrado con el UUID provisto.
     */
    TicketModel payTicket(String uuid);

    /**
     * Valida y procesa la salida de un vehículo mediante su ticket.
     * Verifica que el ticket exista, no haya salido previamente y se encuentre pagado.
     * Al autorizar la salida, decrementa la ocupación y envía la señal de apertura de barrera.
     *
     * @param uuid Identificador único del ticket asociado a la salida.
     * @return {@link ExitResult} con el estado de la operación (éxito con detalles o error descriptivo).
     */
    ExitResult processExit(String uuid);

    /**
     * Crea y registra manualmente un nuevo ticket de estacionamiento con un UUID generado,
     * asignando la hora de entrada actual y actualizando el espacio ocupado si hay disponibilidad.
     *
     * @return El {@link TicketModel} generado y persistido en la base de datos.
     * @throws IllegalStateException Si no existe el parqueo principal configurado.
     */
    TicketModel createManualTicket();
}


