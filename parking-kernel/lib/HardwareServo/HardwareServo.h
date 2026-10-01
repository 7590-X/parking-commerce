/**
 * @file HardwareServo.h
 * @brief Driver de servomotor por Hardware PWM puro (Timer 1) para ATmega328P (Arduino Uno).
 *
 * Utiliza el Timer 1 en modo Fast PWM (50 Hz) conectado físicamente al Pin 9 (OC1A) o Pin 10 (OC1B).
 * Opera a nivel de silicio sin generar interrupciones de software (TIMSK1 = 0),
 * lo que proporciona inmunidad total frente a jitter provocado por librerías
 * que deshabilitan interrupciones como SoftwareSerial.
 */

#pragma once
#include <Arduino.h>

class HardwareServo
{
public:
    HardwareServo();

    /**
     * @brief Inicializa el Timer 1 en modo Fast PWM (50 Hz).
     * @param pin Pin de salida física (en ATmega328P debe ser el Pin 9 / OC1A o Pin 10 / OC1B).
     * @param minPulseUs Ancho de pulso mínimo para 0 grados (por defecto 544 us).
     * @param maxPulseUs Ancho de pulso máximo para 180 grados (por defecto 2400 us).
     */
    void begin(uint8_t pin = 9, uint16_t minPulseUs = 544, uint16_t maxPulseUs = 2400);

    /**
     * @brief Conecta la salida del comparador de hardware al pin físico (Pin 9 u 10).
     * @param pin Pin físico a conectar (255 para reusar el pin configurado en begin).
     */
    void attach(uint8_t pin = 255);

    /**
     * @brief Desconecta el pin del temporizador y lo fija en LOW para reposo total.
     */
    void detach();

    /**
     * @brief Indica si el pin está emitiendo señal PWM activamente.
     * @return true si el periférico está acoplado, false en reposo.
     */
    bool attached() const;

    /**
     * @brief Fija la posición angular del servomotor.
     * @param angle Ángulo en grados [0 - 180].
     */
    void write(uint8_t angle);

    /**
     * @brief Fija el ancho de pulso directamente en microsegundos.
     * @param us Duración del pulso en microsegundos [minPulseUs - maxPulseUs].
     */
    void writeMicroseconds(uint16_t us);

    /**
     * @brief Obtiene el último ángulo comandado al servomotor.
     * @return Ángulo actual en grados [0 - 180].
     */
    uint8_t read() const;

    /**
     * @brief Obtiene el pulso mínimo configurado.
     */
    uint16_t getMinPulseUs() const { return minUs; }

    /**
     * @brief Obtiene el pulso máximo configurado.
     */
    uint16_t getMaxPulseUs() const { return maxUs; }

private:
    uint8_t servoPin;
    uint16_t minUs;
    uint16_t maxUs;
    uint8_t currentAngle;
    bool isAttached;
};
