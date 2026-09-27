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

    // Configurar Pin 9 (PB1 / OC1A) como salida
    pinMode(servoPin, OUTPUT);
    digitalWrite(servoPin, LOW);

    // Configuración del Timer 1 para Fast PWM de 16 bits (Modo 14: TOP = ICR1)
    // Prescaler = 8 -> 16 MHz / 8 = 2 MHz (cada tick equivale a 0.5 µs)
    // Período = 40000 ticks * 0.5 µs = 20,000 µs = 20 ms (50 Hz exactos)
    TCCR1A = _BV(WGM11);
    TCCR1B = _BV(WGM13) | _BV(WGM12) | _BV(CS11);
    ICR1 = 40000;

    // Inhabilitar interrupciones de Timer 1 (modo 100% hardware puro sin interrupción)
    TIMSK1 = 0;

    // Valor inicial de comparación (ancho de pulso mínimo por defecto)
    OCR1A = minUs << 1;
}

void HardwareServo::attach(uint8_t pin)
{
    servoPin = pin;
    pinMode(servoPin, OUTPUT);

    // Conectar el pin 9 (OC1A) directamente a la salida del comparador de hardware
    TCCR1A |= _BV(COM1A1);
    isAttached = true;
}

void HardwareServo::detach()
{
    // Desconectar el pin 9 del Timer 1
    TCCR1A &= ~_BV(COM1A1);

    // Forzar nivel bajo en el pin para reposo absoluto
    digitalWrite(servoPin, LOW);
    isAttached = false;
}

bool HardwareServo::attached() const
{
    return isAttached;
}

void HardwareServo::write(int angle)
{
    if (angle < 0) angle = 0;
    if (angle > 180) angle = 180;
    currentAngle = angle;

    // Conversión lineal precisa de ángulo [0 - 180] a microsegundos [minUs - maxUs]
    uint32_t us = (uint32_t)minUs + (((uint32_t)(maxUs - minUs) * (uint32_t)angle) / 180UL);
    writeMicroseconds((uint16_t)us);
}

void HardwareServo::writeMicroseconds(uint16_t us)
{
    if (us < minUs) us = minUs;
    if (us > maxUs) us = maxUs;

    // Con prescaler 8 y reloj a 16 MHz, cada tick es 0.5 µs:
    // ticks = us / 0.5 = us * 2 (us << 1)
    OCR1A = us << 1;
}

int HardwareServo::read() const
{
    return currentAngle;
}
