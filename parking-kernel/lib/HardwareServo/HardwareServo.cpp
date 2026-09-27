/**
 * @file HardwareServo.cpp
 * @brief Implementación del driver de servomotor por Hardware PWM en ATmega328P.
 */

#include "HardwareServo.h"

HardwareServo::HardwareServo()
    : servoPin(9),
      minUs(544),
      maxUs(2400),
      currentAngle(0),
      isAttached(false)
{
}

void HardwareServo::begin(uint8_t pin, uint16_t minPulseUs, uint16_t maxPulseUs)
{
    servoPin = pin;
    minUs = minPulseUs;
    maxUs = maxPulseUs;
    currentAngle = 0;
    isAttached = false;

    // Configurar pin como salida en nivel bajo
    pinMode(servoPin, OUTPUT);
    digitalWrite(servoPin, LOW);

    // Timer 1: Modo 14 (Fast PWM con TOP = ICR1)
    // Prescaler = 8 -> 16 MHz / 8 = 2 MHz (1 tick = 0.5 µs)
    // Período = 40000 ticks * 0.5 µs = 20,000 µs = 20 ms (50 Hz exactos)
    TCCR1A = _BV(WGM11);
    TCCR1B = _BV(WGM13) | _BV(WGM12) | _BV(CS11);
    ICR1 = 40000;

    // Inhabilitar interrupciones de Timer 1 (inmunidad absoluta contra jitter de software)
    TIMSK1 = 0;

    // Ancho de pulso base inicial
    OCR1A = minUs << 1;
}

void HardwareServo::attach(uint8_t pin)
{
    servoPin = pin;
    pinMode(servoPin, OUTPUT);

    // Conectar el pin físico a la salida del comparador OC1A
    TCCR1A |= _BV(COM1A1);
    isAttached = true;
}

void HardwareServo::detach()
{
    // Desconectar el comparador de hardware de la salida física
    TCCR1A &= ~_BV(COM1A1);

    // Asegurar nivel bajo constante en reposo
    digitalWrite(servoPin, LOW);
    isAttached = false;
}

bool HardwareServo::attached() const
{
    return isAttached;
}

void HardwareServo::write(uint8_t angle)
{
    if (angle > 180)
    {
        angle = 180;
    }
    currentAngle = angle;

    // Mapeo lineal: ángulo [0 - 180] -> microsegundos [minUs - maxUs]
    uint32_t us = (uint32_t)minUs + (((uint32_t)(maxUs - minUs) * (uint32_t)angle) / 180UL);
    writeMicroseconds((uint16_t)us);
}

void HardwareServo::writeMicroseconds(uint16_t us)
{
    if (us < minUs) us = minUs;
    if (us > maxUs) us = maxUs;

    // Conversión a ticks de Timer 1 (2 ticks por microsegundo con reloj a 2 MHz)
    OCR1A = us << 1;
}

uint8_t HardwareServo::read() const
{
    return currentAngle;
}
