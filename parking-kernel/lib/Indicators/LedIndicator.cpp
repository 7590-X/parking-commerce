#include "LedIndicator.h"

// -------------------------------------------------------------
// LedIndicator
// -------------------------------------------------------------
LedIndicator::LedIndicator()
    : pin(255),
      mode(LED_MODE_OFF),
      currentPinState(false),
      intervalMs(0),
      lastToggleTime(0)
{
}

void LedIndicator::begin(uint8_t ledPin)
{
    pin = ledPin;
    pinMode(pin, OUTPUT);
    off();
}

void LedIndicator::on()
{
    mode = LED_MODE_ON;
    currentPinState = true;
    if (pin != 255) digitalWrite(pin, HIGH);
}

void LedIndicator::off()
{
    mode = LED_MODE_OFF;
    currentPinState = false;
    if (pin != 255) digitalWrite(pin, LOW);
}

void LedIndicator::blink(uint32_t blinkInterval)
{
    mode = LED_MODE_BLINK;
    intervalMs = blinkInterval;
    lastToggleTime = millis();
}

void LedIndicator::pulse(uint32_t durationMs)
{
    mode = LED_MODE_ONE_SHOT;
    intervalMs = durationMs;
    currentPinState = true;
    lastToggleTime = millis();
    if (pin != 255) digitalWrite(pin, HIGH);
}

void LedIndicator::update(uint32_t now)
{
    if (pin == 255) return;

    if (mode == LED_MODE_BLINK)
    {
        if (now - lastToggleTime >= intervalMs)
        {
            lastToggleTime = now;
            currentPinState = !currentPinState;
            digitalWrite(pin, currentPinState ? HIGH : LOW);
        }
    }
    else if (mode == LED_MODE_ONE_SHOT)
    {
        if (now - lastToggleTime >= intervalMs)
        {
            off();
        }
    }
}

bool LedIndicator::isOn() const
{
    return currentPinState;
}

LedMode LedIndicator::getMode() const
{
    return mode;
}

// -------------------------------------------------------------
// TrafficLight
// -------------------------------------------------------------
TrafficLight::TrafficLight()
{
}

void TrafficLight::begin(uint8_t greenPin, uint8_t redPin)
{
    greenLed.begin(greenPin);
    redLed.begin(redPin);
    showOccupied();
}

void TrafficLight::update(uint32_t now)
{
    greenLed.update(now);
    redLed.update(now);
}

void TrafficLight::showFree()
{
    greenLed.on();
    redLed.off();
}

void TrafficLight::showOccupied()
{
    greenLed.off();
    redLed.on();
}

void TrafficLight::showMoving()
{
    greenLed.off();
    redLed.blink(LED_BLINK_FAST_MS);
}

void TrafficLight::showWaitAuth()
{
    greenLed.off();
    redLed.blink(LED_BLINK_SLOW_MS);
}

void TrafficLight::allOff()
{
    greenLed.off();
    redLed.off();
}
