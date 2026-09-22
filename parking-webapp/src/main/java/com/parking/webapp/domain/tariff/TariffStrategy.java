package com.parking.webapp.domain.tariff;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Estrategia para el cálculo de tarifas de estacionamiento.
 * Permite aplicar el principio Abierto/Cerrado (Open/Closed) para extender
 * diferentes esquemas de cobro (por hora, nocturno, convenios, etc.) sin modificar el servicio.
 */
public interface TariffStrategy {

    /**
     * Calcula el monto a pagar según la fecha de entrada y la fecha de liquidación.
     *
     * @param entryTime       Momento de ingreso del vehículo.
     * @param calculationTime Momento de liquidación/salida.
     * @return Importe calculado con escala monetaria (2 decimales).
     */
    BigDecimal calculateFee(Instant entryTime, Instant calculationTime);

    /**
     * Retorna una descripción legible de la tarifa aplicada para comprobantes e interfaces.
     *
     * @return Descripción del esquema tarifario.
     */
    String getTariffDescription();
}
