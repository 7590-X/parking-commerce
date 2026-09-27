/**
 * @file BarrierServo.cpp
 * @brief Implementación del controlador de movimiento y reposo de la talanquera.
 */

#include "BarrierServo.h"

BarrierServo::BarrierServo()
    : pin(255),
      currentAngle(BARRIER_ANGLE_CLOSED),
      targetAngle(BARRIER_ANGLE_CLOSED),
      stepIntervalMs(BARRIER_STEP_INTERVAL_MS),
      lastStepTime(0),
      settleStartTime(0),
      settling(false),
      state(BARRIER_STATE_CLOSED)
{
}

void BarrierServo::begin(uint8_t servoPin, uint8_t initialAngle, uint32_t stepInterval)
{
    pin = servoPin;
    currentAngle = initialAngle;
    targetAngle = initialAngle;
    stepIntervalMs = stepInterval;
    lastStepTime = 0;
    settling = true;
    settleStartTime = millis();

    // Inicialización del periférico Hardware PWM y posicionamiento inicial seguro
    servo.begin(pin);
    servo.attach(pin);
    servo.write(currentAngle);
    updateState();

    Serial.print(F("[BARRIER] Inicializada con Hardware PWM en pin "));
    Serial.print(pin);
    Serial.print(F(" | Angulo base: "));
    Serial.print(currentAngle);
    Serial.println(F("°"));
}

void BarrierServo::attachIfNeeded()
{
    if (pin != 255 && !servo.attached())
    {
        servo.attach(pin);
        Serial.print(F("[BARRIER] >>> PWM acoplado (attach) en Pin "));
        Serial.println(pin);
    }
}

void BarrierServo::open()
{
    Serial.println(F("[BARRIER] >>> Comando recibido: APERTURA"));
    setTargetAngle(BARRIER_ANGLE_OPEN);
}

void BarrierServo::close()
{
    Serial.println(F("[BARRIER] >>> Comando recibido: CIERRE"));
    setTargetAngle(BARRIER_ANGLE_CLOSED);
}

void BarrierServo::setTargetAngle(uint8_t angle)
{
    if (angle > 180)
    {
        angle = 180;
    }

    if (targetAngle != angle)
    {
        Serial.print(F("[BARRIER] Cambio de objetivo: "));
        Serial.print(currentAngle);
        Serial.print(F("° -> "));
        Serial.print(angle);
        Serial.println(F("°"));

        targetAngle = angle;
        settling = false;
        attachIfNeeded();
        updateState();
    }
}

void BarrierServo::update(uint32_t now)
{
    if (pin == 255)
    {
        return;
    }

    // 1. Desplazamiento angular paso a paso (grado a grado)
    if (currentAngle != targetAngle)
    {
        attachIfNeeded();

        if (now - lastStepTime >= stepIntervalMs)
        {
            lastStepTime = now;

            if (currentAngle < targetAngle)
            {
                currentAngle++;
            }
            else if (currentAngle > targetAngle)
            {
                currentAngle--;
            }

            servo.write(currentAngle);
            updateState();

            // Al alcanzar el ángulo objetivo, iniciar la fase de asentamiento físico
            if (currentAngle == targetAngle)
            {
                settling = true;
                settleStartTime = now;
            }
        }
    }
    // 2. Fase de reposo y desacoplamiento de señal
    else if (settling)
    {
        if (now - settleStartTime >= BARRIER_SETTLE_MS)
        {
            settling = false;
            if (servo.attached())
            {
                servo.detach(); // Corte total de señal PWM para eliminar ruido en reposo
                Serial.print(F("[BARRIER] <<< Reposo alcanzado en "));
                Serial.print(currentAngle);
                Serial.println(F("°. PWM desacoplado (detach)."));
            }
            updateState();
        }
    }
}

void BarrierServo::updateState()
{
    if (currentAngle < targetAngle)
    {
        state = BARRIER_STATE_OPENING;
    }
    else if (currentAngle > targetAngle)
    {
        state = BARRIER_STATE_CLOSING;
    }
    else
    {
        state = (currentAngle >= BARRIER_ANGLE_OPEN) ? BARRIER_STATE_OPEN : BARRIER_STATE_CLOSED;
    }
}

BarrierState BarrierServo::getState() const
{
    return state;
}

bool BarrierServo::isMoving() const
{
    return (state == BARRIER_STATE_OPENING || state == BARRIER_STATE_CLOSING || settling);
}

bool BarrierServo::isFullyOpen() const
{
    return (state == BARRIER_STATE_OPEN && !settling);
}

bool BarrierServo::isFullyClosed() const
{
    return (state == BARRIER_STATE_CLOSED && !settling);
}

uint8_t BarrierServo::getCurrentAngle() const
{
    return currentAngle;
}
