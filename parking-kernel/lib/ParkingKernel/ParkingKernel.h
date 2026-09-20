#pragma once
#include <Arduino.h>
#include <Config.h>
#include <UltrasonicSensor.h>
#include <BarrierServo.h>
#include <LedIndicator.h>
#include <BrokerLib.h>

enum KernelState : uint8_t
{
    STATE_IDLE = 0,             // Talanquera cerrada, esperando vehículo
    STATE_VEHICLE_WAIT_AUTH,    // Vehículo detectado, esperando validación de espacio por MQTT
    STATE_ACCESS_DENIED,        // No hay espacio disponible (rechazado por el broker)
    STATE_OPENING,              // Talanquera abriéndose (autorizado)
    STATE_OPEN_WAIT_PASS,       // Talanquera abierta, vehículo cruzando
    STATE_CLEARING_DELAY,       // Vehículo ya cruzó, retardo de seguridad antes de cerrar
    STATE_CLOSING               // Talanquera cerrándose
};

class ParkingKernel
{
public:
    ParkingKernel();

    void begin();
    void update(uint32_t now);

    // Procesa respuestas del broker sobre disponibilidad de espacio ("ALLOW", "DENY")
    void handleAuthResponse(const char *response);

    // Procesa comandos manuales o de emergencia de MQTT ("OPEN", "CLOSE")
    void handleCommand(const char *cmd);

    // Modo automático (si true, abre al detectar sin consultar al broker)
    void setAutoOpen(bool enabled);
    bool isAutoOpen() const;

    KernelState getState() const;

private:
    UltrasonicSensor entranceSensor;
    BarrierServo entranceBarrier;
    TrafficLight entranceLight;

    KernelState currentState;
    bool autoOpen;
    uint32_t stateTimer;
    uint32_t lastTelemetryTime;

    void transitionTo(KernelState newState);
    void sendTelemetry(uint32_t now);
};
