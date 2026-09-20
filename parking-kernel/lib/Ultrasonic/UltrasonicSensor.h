#pragma once
#include <Arduino.h>
#include <Config.h>

class UltrasonicSensor
{
public:
    UltrasonicSensor();

    void begin(uint8_t trigPin, uint8_t echoPin,
               uint16_t detectThresholdCm = US_DETECT_THRESHOLD_CM,
               uint32_t sampleIntervalMs = US_SAMPLE_INTERVAL_MS,
               uint32_t timeoutUs = US_TIMEOUT_US);

    // Debe llamarse en el super-loop periódicamente
    void update(uint32_t now);

    // Consultas de estado
    bool isVehiclePresent() const;
    uint16_t getDistanceCm() const;

    // Eventos (se activan solo en el ciclo donde ocurre la transición)
    bool hasVehicleArrived();
    bool hasVehicleCleared();

private:
    uint8_t trigPin;
    uint8_t echoPin;
    uint16_t thresholdCm;
    uint32_t sampleIntervalMs;
    uint32_t timeoutUs;

    uint32_t lastSampleTime;
    uint16_t lastDistanceCm;

    bool vehiclePresent;
    uint8_t debounceCounter;

    bool arrivedEvent;
    bool clearedEvent;

    uint16_t readDistanceOnce();
};
