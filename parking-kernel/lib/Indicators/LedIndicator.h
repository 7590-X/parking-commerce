/**
 * @file LedIndicator.h
 * @brief Gestión de señalización visual con LEDs y semáforo vehicular bicolor.
 *
 * Ofrece control no bloqueante para encendido, apagado, pulsos temporizados
 * y parpadeo continuo, así como abstracción de semáforo para el control de acceso.
 */

#pragma once
#include <Arduino.h>
#include <Config.h>

/**
 * @brief Modos operativos del indicador luminoso LED.
 */
enum LedMode : uint8_t
{
    LED_MODE_OFF = 0,   ///< Apagado continuo
    LED_MODE_ON,        ///< Encendido continuo
    LED_MODE_BLINK,     ///< Parpadeo simétrico no bloqueante
    LED_MODE_ONE_SHOT   ///< Pulso único temporizado
};

class LedIndicator
{
public:
    LedIndicator();

    /**
     * @brief Configura el pin del LED como salida y fija el estado inicial en apagado.
     * @param pin Pin digital de Arduino.
     */
    void begin(uint8_t pin);

    /**
     * @brief Actualiza la máquina de estados del parpadeo y temporización del LED.
     * @param now Tiempo actual en milisegundos.
     */
    void update(uint32_t now);

    /**
     * @brief Enciende el LED de forma permanente.
     */
    void on();

    /**
     * @brief Apaga el LED.
     */
    void off();

    /**
     * @brief Inicia parpadeo continuo con el período especificado.
     * @param intervalMs Período de conmutación en milisegundos.
     */
    void blink(uint32_t intervalMs = LED_BLINK_SLOW_MS);

    /**
     * @brief Genera un pulso único encendido durante durationMs.
     * @param durationMs Duración del pulso en milisegundos.
     */
    void pulse(uint32_t durationMs = 300UL);

    /**
     * @brief Consulta si el pin del LED está actualmente en HIGH.
     */
    bool isOn() const;

    /**
     * @brief Retorna el modo operativo actual del LED.
     */
    LedMode getMode() const;

private:
    uint8_t pin;
    LedMode mode;
    bool currentPinState;
    uint32_t intervalMs;
    uint32_t lastToggleTime;
};

/**
 * @brief Controlador del semáforo de acceso vehicular (LED Verde y LED Rojo).
 */
class TrafficLight
{
public:
    TrafficLight();

    /**
     * @brief Inicializa los dos canales LED del semáforo.
     */
    void begin(uint8_t greenPin, uint8_t redPin);

    /**
     * @brief Actualiza el parpadeo de ambos LEDs en cada ciclo cooperativo.
     */
    void update(uint32_t now);

    /**
     * @brief Señal Verde fija (acceso permitido, paso libre).
     */
    void showFree();

    /**
     * @brief Señal Roja fija (estacionamiento ocupado / alto).
     */
    void showOccupied();

    /**
     * @brief Señal Roja con parpadeo rápido (talanquera en movimiento o rechazo).
     */
    void showMoving();

    /**
     * @brief Señal Roja con parpadeo lento (esperando respuesta de autorización del broker).
     */
    void showWaitAuth();

    /**
     * @brief Apaga ambos indicadores.
     */
    void allOff();

private:
    LedIndicator greenLed;
    LedIndicator redLed;
};
