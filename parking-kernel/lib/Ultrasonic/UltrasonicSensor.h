/**
 * @file UltrasonicSensor.h
 * @brief Driver del sensor ultrasónico HC-SR04 con anti-rebote y temporización acotada.
 *
 * Realiza mediciones de distancia acústica sin bloquear el bucle principal gracias a un
 * timeout ajustado en pulseIn (~5.8 ms para 1 metro), integrando un filtro anti-rebote
 * configurable para descartar perturbaciones espurias.
 */

#pragma once
#include <Arduino.h>
#include <Config.h>

class UltrasonicSensor
{
public:
    UltrasonicSensor();

    /**
     * @brief Configura los pines de disparo y eco, umbrales y tiempos de muestreo.
     * @param trigPin Pin digital conectado a TRIGGER (salida).
     * @param echoPin Pin digital conectado a ECHO (entrada).
     * @param detectThresholdCm Umbral de presencia vehicular en centímetros.
     * @param sampleIntervalMs Intervalo entre mediciones acústicas.
     * @param timeoutUs Tiempo máximo de espera del eco en microsegundos.
     */
    void begin(uint8_t trigPin, uint8_t echoPin,
               uint16_t detectThresholdCm = US_DETECT_THRESHOLD_CM,
               uint32_t sampleIntervalMs = US_SAMPLE_INTERVAL_MS,
               uint32_t timeoutUs = US_TIMEOUT_US);

    /**
     * @brief Ejecuta el muestreo periódico y la máquina de anti-rebote.
     * @param now Tiempo actual en milisegundos.
     */
    void update(uint32_t now);

    /**
     * @brief Consulta si hay un vehículo presente de forma sostenida frente al sensor.
     */
    bool isVehiclePresent() const;

    /**
     * @brief Retorna la última distancia medida en centímetros (999 si no hay eco).
     */
    uint16_t getDistanceCm() const;

    /**
     * @brief Evento de llegada: retorna true exactamente en el ciclo donde se confirma la presencia.
     */
    bool hasVehicleArrived();

    /**
     * @brief Evento de cruce/despeje: retorna true en el ciclo donde el vehículo se retira.
     */
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
