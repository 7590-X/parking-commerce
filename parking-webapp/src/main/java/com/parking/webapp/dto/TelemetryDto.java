package com.parking.webapp.dto;

import java.time.Instant;

public record TelemetryDto(
        short max,
        short current,
        Instant lastUpdated) {
}
