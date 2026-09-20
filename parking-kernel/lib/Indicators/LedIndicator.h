#pragma once
#include <Arduino.h>
#include <Config.h>

enum LedMode : uint8_t
{
    LED_MODE_OFF = 0,
    LED_MODE_ON,
    LED_MODE_BLINK,
    LED_MODE_ONE_SHOT
};

class LedIndicator
{
public:
    LedIndicator();

    void begin(uint8_t pin);
    void update(uint32_t now);

    void on();
    void off();
    void blink(uint32_t intervalMs = LED_BLINK_SLOW_MS);
    void pulse(uint32_t durationMs = 300UL);

    bool isOn() const;
    LedMode getMode() const;

private:
    uint8_t pin;
    LedMode mode;
    bool currentPinState;
    uint32_t intervalMs;
    uint32_t lastToggleTime;
};

// Controlador de Semáforo / Indicador Bicolor (Verde / Rojo)
class TrafficLight
{
public:
    TrafficLight();

    void begin(uint8_t greenPin, uint8_t redPin);
    void update(uint32_t now);

    void showFree();       // Verde continuo, Rojo apagado
    void showOccupied();   // Rojo continuo, Verde apagado
    void showMoving();     // Rojo parpadeo rápido (precaución talanquera)
    void showWaitAuth();   // Rojo parpadeo lento (esperando validación)
    void allOff();

private:
    LedIndicator greenLed;
    LedIndicator redLed;
};
