#pragma once
#include <Arduino.h>
#include <Servo.h>
#include <Config.h>

enum BarrierState : uint8_t
{
    BARRIER_STATE_CLOSED = 0,
    BARRIER_STATE_OPENING,
    BARRIER_STATE_OPEN,
    BARRIER_STATE_CLOSING
};

class BarrierServo
{
public:
    BarrierServo();

    void begin(uint8_t pin, int initialAngle = BARRIER_ANGLE_CLOSED, uint32_t stepIntervalMs = BARRIER_STEP_INTERVAL_MS);
    void update(uint32_t now);

    void open();
    void close();
    void setTargetAngle(int angle);

    BarrierState getState() const;
    bool isMoving() const;
    bool isFullyOpen() const;
    bool isFullyClosed() const;
    int getCurrentAngle() const;

private:
    Servo servo;
    uint8_t pin;
    int currentAngle;
    int targetAngle;
    uint32_t stepIntervalMs;
    uint32_t lastStepTime;
    BarrierState state;

    void updateState();
};
