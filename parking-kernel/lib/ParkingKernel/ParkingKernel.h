/**
 * @file ParkingKernel.h
 * @brief Orquestador central y Máquina de Estados Finitos (FSM) de control de acceso vehicular.
 *
 * Integra y coordina el sensor ultrasónico, la talanquera con Hardware PWM, el semáforo LED
 * y los eventos de telemetría/autorización bidireccionales vía MQTT.
 */

#pragma once
#include <Arduino.h>
#include <Config.h>
#include <UltrasonicSensor.h>
#include <BarrierServo.h>
#include <LedIndicator.h>
#include <BrokerLib.h>

/**
 * @brief Estados del flujo de control de acceso vehicular.
 */
enum KernelState : uint8_t
{
    STATE_IDLE = 0,             ///< Talanquera cerrada, semáforo rojo, esperando aproximación vehicular
    STATE_VEHICLE_WAIT_AUTH,    ///< Vehículo detectado; esperando validación de cupo desde el broker MQTT
    STATE_ACCESS_DENIED,        ///< Rechazo de acceso (estacionamiento sin espacio libre)
    STATE_OPENING,              ///< Elevando talanquera tras autorización concedida
    STATE_OPEN_WAIT_PASS,       ///< Talanquera totalmente elevada, semáforo verde, vehículo cruzando
    STATE_CLEARING_DELAY,       ///< Vehículo cruzó completamente; retardo de seguridad antes de iniciar descenso
    STATE_CLOSING               ///< Descendiendo talanquera (con protección anti-aplastamiento activa)
};

/**
 * @brief Estados del flujo de salida vehicular independiente.
 */
enum ExitBarrierState : uint8_t
{
    EXIT_STATE_IDLE = 0,        ///< Talanquera de salida cerrada en reposo
    EXIT_STATE_OPENING,         ///< Elevando talanquera de salida
    EXIT_STATE_OPEN_WAIT,       ///< Talanquera elevada, tiempo de cortesía de paso activo
    EXIT_STATE_CLOSING          ///< Descendiendo talanquera de salida
};

class ParkingKernel
{
public:
    ParkingKernel();

    /**
     * @brief Inicializa los subsistemas de sensores, actuadores y señalización.
     */
    void begin();

    /**
     * @brief Ciclo cooperativo principal: actualiza periféricos, FSM y telemetría periódica.
     * @param now Tiempo actual en milisegundos.
     */
    void update(uint32_t now);

    /**
     * @brief Procesa respuestas del broker sobre disponibilidad de espacio ("ALLOW", "DENY").
     * @param response Payload recibido desde el backend.
     */
    void handleAuthResponse(const char *response);

    /**
     * @brief Procesa respuestas del broker sobre autorización de salida ("ALLOW").
     * @param response Payload recibido desde el backend.
     */
    void handleExitResponse(const char *response);

    /**
     * @brief Procesa comandos manuales o de emergencia ("OPEN", "CLOSE", "AUTO_ON", "AUTO_OFF").
     * @param cmd Comando recibido.
     */
    void handleCommand(const char *cmd);

    /**
     * @brief Configura el modo de apertura automática sin consulta al broker.
     */
    void setAutoOpen(bool enabled);

    /**
     * @brief Consulta si el modo de apertura automática está activo.
     */
    bool isAutoOpen() const;

    /**
     * @brief Retorna el estado actual de la máquina de estados.
     */
    KernelState getState() const;

    /**
     * @brief Retorna el estado actual de la talanquera de salida.
     */
    ExitBarrierState getExitState() const { return exitState; }

private:
    UltrasonicSensor entranceSensor;
    BarrierServo entranceBarrierIn;
    BarrierServo exitBarrierOut;
    TrafficLight entranceLight;

    KernelState currentState;
    ExitBarrierState exitState;
    bool autoOpen;
    uint32_t stateTimer;
    uint32_t exitStateTimer;
    uint32_t lastTelemetryTime;

    void transitionTo(KernelState newState);
    void sendTelemetry(uint32_t now);
};
