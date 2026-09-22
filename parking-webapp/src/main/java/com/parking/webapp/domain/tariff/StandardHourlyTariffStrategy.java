package com.parking.webapp.domain.tariff;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

/**
 * Estrategia de tarifa estándar por hora:
 * - Tarifa base: Q10.00 (primera hora o fracción).
 * - Tarifa adicional: Q5.00 por cada hora adicional o fracción.
 */
@Component
public class StandardHourlyTariffStrategy implements TariffStrategy {

    private static final BigDecimal BASE_FEE = new BigDecimal("10.00");
    private static final BigDecimal EXTRA_HOUR_FEE = new BigDecimal("5.00");

    @Override
    public BigDecimal calculateFee(Instant entryTime, Instant calculationTime) {
        if (entryTime == null) {
            entryTime = Instant.now();
        }
        if (calculationTime == null) {
            calculationTime = Instant.now();
        }

        if (calculationTime.isBefore(entryTime)) {
            calculationTime = entryTime;
        }

        long minutes = Math.max(1, Duration.between(entryTime, calculationTime).toMinutes());
        long billableHours = (long) Math.ceil(minutes / 60.0);

        BigDecimal extraHours = BigDecimal.valueOf(Math.max(0, billableHours - 1));
        BigDecimal total = BASE_FEE.add(extraHours.multiply(EXTRA_HOUR_FEE));

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getTariffDescription() {
        return "Tarifa Estándar: Q10.00 (1ra hora) + Q5.00 por cada hora adicional";
    }
}
