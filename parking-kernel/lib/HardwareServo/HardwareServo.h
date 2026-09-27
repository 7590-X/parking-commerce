#pragma once
#include <Arduino.h>

/**
 * @brief Driver de servomotor por Hardware PWM puro (Timer 1) para ATmega328P (Arduino Uno).
 * 
 * Genera pulsos de 50 Hz en el Pin 9 (OC1A) directamente a nivel de silicio sin utilizar
 * interrupciones de software. Esto garantiza inmunidad total (0% jitter) frente a interrupciones
 * bloqueantes como las de SoftwareSerial utilizadas por módulos WiFi/ESP8266.
 */
class HardwareServo
{
public:
    HardwareServo();

    /**
     * @brief Inicializa el Timer 1 en modo Fast PWM (50 Hz) en el pin de hardware correspondiente.
     * @param pin Pin físico (en ATmega328P debe ser el Pin 9 / OC1A).
     * @param minPulseUs Ancho de pulso mínimo para 0 grados (por defecto 544 us).
     * @param maxPulseUs Ancho de pulso máximo para 180 grados (por defecto 2400 us).
     */
    void begin(uint8_t pin = 9, uint16_t minPulseUs = 544, uint16_t maxPulseUs = 2400);

    /**
     * @brief Conecta la señal PWM física al pin de salida.
     */
    void attach(uint8_t pin = 9);

    /**
     * @brief Desconecta el pin del generador PWM y lo coloca en LOW (reposo total).
     */
    void detach();

    /**
     * @brief Indica si el pin está actualmente emitiendo señal PWM.
     */
    bool attached() const;

    /**
     * @brief Fija la posición angular del servomotor.
     * @param angle Ángulo en grados [0 - 180].
     */
    void write(int angle);

    /**
     * @brief Fija el ancho de pulso directamente en microsegundos.
     * @param us Ancho en microsegundos [minPulseUs - maxPulseUs].
     */
    void writeMicroseconds(uint16_t us);

    /**
     * @brief Obtiene el último ángulo ordenado al servo.
     */
    int read() const;

private:
    uint8_t servoPin;
    uint16_t minUs;
    uint16_t maxUs;
    int currentAngle;
    bool isAttached;
};
