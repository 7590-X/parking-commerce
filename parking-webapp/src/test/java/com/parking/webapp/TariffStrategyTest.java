package com.parking.webapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.parking.webapp.domain.tariff.StandardHourlyTariffStrategy;

class TariffStrategyTest {

    private StandardHourlyTariffStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new StandardHourlyTariffStrategy();
    }

    @Test
    void testFirstHourCharge() {
        Instant now = Instant.now();
        Instant entryTime = now.minus(30, ChronoUnit.MINUTES);

        BigDecimal fee = strategy.calculateFee(entryTime, now);

        assertEquals(new BigDecimal("10.00"), fee);
    }

    @Test
    void testSecondHourCharge() {
        Instant now = Instant.now();
        Instant entryTime = now.minus(65, ChronoUnit.MINUTES);

        BigDecimal fee = strategy.calculateFee(entryTime, now);

        assertEquals(new BigDecimal("15.00"), fee);
    }

    @Test
    void testThreeHoursCharge() {
        Instant now = Instant.now();
        Instant entryTime = now.minus(150, ChronoUnit.MINUTES);

        BigDecimal fee = strategy.calculateFee(entryTime, now);

        assertEquals(new BigDecimal("20.00"), fee);
    }

    @Test
    void testNullEntryFallback() {
        Instant now = Instant.now();
        BigDecimal fee = strategy.calculateFee(null, now);

        assertEquals(new BigDecimal("10.00"), fee);
    }

    @Test
    void testDescription() {
        assertNotNull(strategy.getTariffDescription());
    }
}
