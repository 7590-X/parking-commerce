#include "ParkingKernel.h"

ParkingKernel::ParkingKernel()
    : currentState(STATE_IDLE),
      autoOpen(false), // Requiere validación por MQTT del broker por defecto
      stateTimer(0),
      lastTelemetryTime(0)
{
}

void ParkingKernel::begin()
{
    entranceSensor.begin(PIN_US_TRIG, PIN_US_ECHO, US_DETECT_THRESHOLD_CM, US_SAMPLE_INTERVAL_MS, US_TIMEOUT_US);
    entranceBarrier.begin(PIN_SERVO_BARRIER, BARRIER_ANGLE_CLOSED, BARRIER_STEP_INTERVAL_MS);
    entranceLight.begin(PIN_LED_GREEN, PIN_LED_RED);

    transitionTo(STATE_IDLE);
    Serial.println(F("[KERNEL] Sistema de Parqueo inicializado correctamente."));
}

void ParkingKernel::setAutoOpen(bool enabled)
{
    autoOpen = enabled;
    Serial.print(F("[KERNEL] Modo auto-apertura: "));
    Serial.println(autoOpen ? F("ACTIVADO") : F("DESACTIVADO (Requiere Broker)"));
}

bool ParkingKernel::isAutoOpen() const
{
    return autoOpen;
}

KernelState ParkingKernel::getState() const
{
    return currentState;
}

void ParkingKernel::transitionTo(KernelState newState)
{
    currentState = newState;
    stateTimer = millis();

    switch (currentState)
    {
    case STATE_IDLE:
        entranceBarrier.close();
        entranceLight.showOccupied();
        Serial.println(F("[KERNEL] Estado: IDLE (Talanquera cerrada, semaforo rojo)"));
        break;

    case STATE_VEHICLE_WAIT_AUTH:
        entranceLight.showWaitAuth();
        Serial.println(F("[KERNEL] Estado: VEHICULO DETECTADO -> Solicitando validacion de espacio al Broker..."));
        sendMQTTMessage(TOPIC_ENTRY_REQUEST, PAYLOAD_CHECK_SPACE);
        break;

    case STATE_ACCESS_DENIED:
        entranceBarrier.close();
        entranceLight.showMoving(); // Alerta visual de rechazo
        Serial.println(F("[KERNEL] Estado: ACCESO DENEGADO (Sin espacio disponible en parqueo)"));
        sendMQTTMessage(TOPIC_SENSOR_EVENT, "ACCESS_DENIED_NO_SPACE");
        break;

    case STATE_OPENING:
        entranceBarrier.open();
        entranceLight.showMoving();
        Serial.println(F("[KERNEL] Estado: ABRIENDO TALANQUERA (Acceso autorizado)"));
        sendMQTTMessage(TOPIC_BARRIER_STATE, "OPENING");
        break;

    case STATE_OPEN_WAIT_PASS:
        entranceLight.showFree();
        Serial.println(F("[KERNEL] Estado: TALANQUERA ABIERTA (Semaforo verde, pase libre)"));
        sendMQTTMessage(TOPIC_BARRIER_STATE, "OPEN");
        break;

    case STATE_CLEARING_DELAY:
        Serial.println(F("[KERNEL] Estado: RETARDO DE SEGURIDAD (Vehiculo cruzo)"));
        break;

    case STATE_CLOSING:
        entranceBarrier.close();
        entranceLight.showMoving();
        Serial.println(F("[KERNEL] Estado: CERRANDO TALANQUERA"));
        sendMQTTMessage(TOPIC_BARRIER_STATE, "CLOSING");
        break;
    }
}

void ParkingKernel::handleAuthResponse(const char *response)
{
    Serial.print(F("[KERNEL] Respuesta de Broker recibida: "));
    Serial.println(response);

    // Procesar solo si estamos esperando autorización o si fuimos denegados previamente
    if (currentState == STATE_VEHICLE_WAIT_AUTH || currentState == STATE_ACCESS_DENIED)
    {
        if (strcmp(response, PAYLOAD_ALLOW) == 0 || strcmp(response, "OPEN") == 0 || strcmp(response, "VLD") == 0)
        {
            Serial.println(F("[KERNEL] Espacio CONFIRMADO -> Autorizando ingreso"));
            transitionTo(STATE_OPENING);
        }
        else if (strcmp(response, PAYLOAD_DENY) == 0 || strcmp(response, "FULL") == 0)
        {
            Serial.println(F("[KERNEL] Espacio DENEGADO -> No hay plazas libres"));
            transitionTo(STATE_ACCESS_DENIED);
        }
    }
}

void ParkingKernel::handleCommand(const char *cmd)
{
    if (strcmp(cmd, "OPEN") == 0)
    {
        Serial.println(F("[KERNEL] Comando manual recibido: OPEN"));
        transitionTo(STATE_OPENING);
    }
    else if (strcmp(cmd, "CLOSE") == 0)
    {
        Serial.println(F("[KERNEL] Comando manual recibido: CLOSE"));
        transitionTo(STATE_CLOSING);
    }
    else if (strcmp(cmd, "AUTO_ON") == 0)
    {
        setAutoOpen(true);
    }
    else if (strcmp(cmd, "AUTO_OFF") == 0)
    {
        setAutoOpen(false);
    }
}

void ParkingKernel::update(uint32_t now)
{
    // 1. Actualización no bloqueante de periféricos
    entranceSensor.update(now);
    entranceBarrier.update(now);
    entranceLight.update(now);

    // 2. Máquina de estados finitos (FSM)
    switch (currentState)
    {
    case STATE_IDLE:
        if (entranceSensor.hasVehicleArrived())
        {
            if (autoOpen)
            {
                transitionTo(STATE_OPENING);
            }
            else
            {
                transitionTo(STATE_VEHICLE_WAIT_AUTH);
            }
        }
        break;

    case STATE_VEHICLE_WAIT_AUTH:
        // Si el vehículo se retira antes de recibir respuesta
        if (entranceSensor.hasVehicleCleared())
        {
            Serial.println(F("[KERNEL] Vehiculo se retiro mientras esperaba respuesta. Cancelando."));
            sendMQTTMessage(TOPIC_SENSOR_EVENT, "REQUEST_CANCELLED");
            transitionTo(STATE_IDLE);
        }
        // Timeout de espera si el broker no responde
        else if (now - stateTimer >= AUTH_TIMEOUT_MS)
        {
            Serial.println(F("[KERNEL] TIMEOUT esperando respuesta del Broker. Cancelando solicitud."));
            sendMQTTMessage(TOPIC_SENSOR_EVENT, "AUTH_TIMEOUT");
            transitionTo(STATE_IDLE);
        }
        break;

    case STATE_ACCESS_DENIED:
        // Después de la alerta visual de rechazo, mantener luz roja continua
        if (now - stateTimer >= REJECTION_ALERT_MS)
        {
            entranceLight.showOccupied();
        }

        // Si el vehículo se retira tras ser denegado
        if (entranceSensor.hasVehicleCleared())
        {
            Serial.println(F("[KERNEL] Vehiculo denegado se ha retirado. Volviendo a IDLE."));
            transitionTo(STATE_IDLE);
        }
        break;

    case STATE_OPENING:
        if (entranceBarrier.isFullyOpen())
        {
            transitionTo(STATE_OPEN_WAIT_PASS);
        }
        break;

    case STATE_OPEN_WAIT_PASS:
        // Esperar a que el vehículo cruce completamente el sensor
        if (entranceSensor.hasVehicleCleared())
        {
            sendMQTTMessage(TOPIC_SENSOR_EVENT, "VEHICLE_CLEARED");
            transitionTo(STATE_CLEARING_DELAY);
        }
        break;

    case STATE_CLEARING_DELAY:
        // Seguridad: si otro vehículo se acerca inmediatamente, volvemos a esperar cruce
        if (entranceSensor.isVehiclePresent())
        {
            transitionTo(STATE_OPEN_WAIT_PASS);
        }
        else if (now - stateTimer >= BARRIER_AUTO_CLOSE_MS)
        {
            transitionTo(STATE_CLOSING);
        }
        break;

    case STATE_CLOSING:
        // Seguridad anti-aplastamiento: si un vehículo aparece mientras cierra, reabrir de inmediato
        if (entranceSensor.isVehiclePresent())
        {
            Serial.println(F("[KERNEL] SEGURIDAD: Obstaculo detectado al cerrar -> Reabriendo!"));
            sendMQTTMessage(TOPIC_SENSOR_EVENT, "OBSTACLE_DETECTED_REOPENING");
            transitionTo(STATE_OPENING);
        }
        else if (entranceBarrier.isFullyClosed())
        {
            sendMQTTMessage(TOPIC_BARRIER_STATE, "CLOSED");
            transitionTo(STATE_IDLE);
        }
        break;
    }

    // 3. Telemetría periódica no saturante (cada N segundos)
    sendTelemetry(now);
}

void ParkingKernel::sendTelemetry(uint32_t now)
{
    if (now - lastTelemetryTime >= TELEMETRY_INTERVAL_MS)
    {
        lastTelemetryTime = now;

        if (isMQTTConnected())
        {
            char buffer[32];
            snprintf(buffer, sizeof(buffer), "dist=%u,state=%u,ang=%d",
                     entranceSensor.getDistanceCm(),
                     (uint8_t)currentState,
                     entranceBarrier.getCurrentAngle());
            sendMQTTMessage(TOPIC_TELEMETRY, buffer);
        }
    }
}
