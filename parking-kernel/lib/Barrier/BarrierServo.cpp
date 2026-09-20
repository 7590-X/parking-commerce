#include "BarrierServo.h"

BarrierServo::BarrierServo()
    : pin(255),
      currentAngle(BARRIER_ANGLE_CLOSED),
      targetAngle(BARRIER_ANGLE_CLOSED),
      stepIntervalMs(BARRIER_STEP_INTERVAL_MS),
      lastStepTime(0),
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

    servo.attach(pin);
    servo.write(currentAngle);
    updateState();
}

void BarrierServo::open()
{
    setTargetAngle(BARRIER_ANGLE_OPEN);
}

void BarrierServo::close()
{
    setTargetAngle(BARRIER_ANGLE_CLOSED);
}

void BarrierServo::setTargetAngle(int angle)
{
    if (angle < 0) angle = 0;
    if (angle > 180) angle = 180;
    targetAngle = angle;
    updateState();
}

void BarrierServo::update(uint32_t now)
{
    if (currentAngle == targetAngle)
    {
        updateState();
        return;
    }

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
    return (state == BARRIER_STATE_OPENING || state == BARRIER_STATE_CLOSING);
}

bool BarrierServo::isFullyOpen() const
{
    return (state == BARRIER_STATE_OPEN);
}

bool BarrierServo::isFullyClosed() const
{
    return (state == BARRIER_STATE_CLOSED);
}

int BarrierServo::getCurrentAngle() const
{
    return currentAngle;
}
