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

void BarrierServo::begin(uint8_t servoPin, int initialAngle, uint32_t stepInterval)
{
    pin = servoPin;
    currentAngle = initialAngle;
    targetAngle = initialAngle;
    stepIntervalMs = stepInterval;
    lastStepTime = 0;
    settling = true;
    settleStartTime = millis();

    // Posicionamiento inicial seguro mediante Hardware PWM
    servo.begin(pin);
    servo.attach(pin);
    servo.write(currentAngle);
    updateState();

    Serial.print(F("[TALANQUERA] Inicializada con Hardware PWM en pin "));
    Serial.print(pin);
    Serial.print(F(" | Angulo inicial: "));
    Serial.print(currentAngle);
    Serial.println(F("°"));
}

void BarrierServo::attachIfNeeded()
{
    if (pin != 255 && !servo.attached())
    {
        servo.attach(pin);
        Serial.print(F("[TALANQUERA] >>> SENAL ACTIVADA: PWM acoplado (attach) en Pin "));
        Serial.println(pin);
    }
}

void BarrierServo::open()
{
    Serial.println(F("[TALANQUERA] >>> INICIANDO APERTURA (Comando recibido)"));
    setTargetAngle(BARRIER_ANGLE_OPEN);
}

void BarrierServo::close()
{
    Serial.println(F("[TALANQUERA] >>> INICIANDO CIERRE (Comando recibido)"));
    setTargetAngle(BARRIER_ANGLE_CLOSED);
}

void BarrierServo::setTargetAngle(int angle)
{
    if (angle < 0) angle = 0;
    if (angle > 180) angle = 180;

    if (targetAngle != angle)
    {
        Serial.print(F("[TALANQUERA] Cambio de objetivo: "));
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
    if (pin == 255) return;

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

            // Si acabamos de llegar al ángulo objetivo, iniciar tiempo de asentamiento físico
            if (currentAngle == targetAngle)
            {
                settling = true;
                settleStartTime = now;
            }
        }
    }
    else
    {
        // currentAngle == targetAngle
        if (settling)
        {
            if (now - settleStartTime >= BARRIER_SETTLE_MS)
            {
                settling = false;
                if (servo.attached())
                {
                    servo.detach(); // Corte total de señal PWM en reposo para eliminar jitter
                    Serial.print(F("[TALANQUERA] <<< REPOSO ALCANZADO: "));
                    Serial.print(currentAngle);
                    Serial.println(F("°. PWM desacoplado (detach)."));
                }
                updateState();
            }
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
        if (currentAngle >= BARRIER_ANGLE_OPEN)
        {
            state = BARRIER_STATE_OPEN;
        }
        else
        {
            state = BARRIER_STATE_CLOSED;
        }
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

int BarrierServo::getCurrentAngle() const
{
    return currentAngle;
}
