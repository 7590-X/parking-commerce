/**
 * @file BarrierServo.h
 * @brief Controlador de alto nivel de la talanquera vehicular (movimiento suave y reposo seguro).
 *
 * Administra el ángulo de la barrera de forma cooperativa y no bloqueante mediante
 * pasos angulares temporizados y desacoplamiento automático (detach) en reposo.
 */

#pragma once
#include <Arduino.h>
#include <HardwareServo.h>
#include <Config.h>

/**
 * @brief Estados físicos y operativos de la talanquera.
 */
enum BarrierState : uint8_t
{
    BARRIER_STATE_CLOSED = 0,   ///< Talanquera horizontal (cerrada)
    BARRIER_STATE_OPENING,      ///< Talanquera en proceso de elevación
    BARRIER_STATE_OPEN,         ///< Talanquera vertical (abierta)
    BARRIER_STATE_CLOSING       ///< Talanquera en proceso de descenso
};

class BarrierServo
{
public:
    BarrierServo();

    /**
     * @brief Inicializa el controlador de la talanquera y posiciona en el ángulo base.
     * @param pin Pin de salida física (Pin 9).
     * @param initialAngle Ángulo de reposo cerrado inicial.
     * @param stepIntervalMs Intervalo en milisegundos entre cada grado de avance.
     */
    void begin(uint8_t pin, uint8_t initialAngle = BARRIER_ANGLE_CLOSED, uint32_t stepIntervalMs = BARRIER_STEP_INTERVAL_MS);

    /**
     * @brief Actualiza periódicamente el movimiento paso a paso y el temporizador de asentamiento.
     * @param now Tiempo actual en milisegundos (millis()).
     */
    void update(uint32_t now);

    /**
     * @brief Comanda la apertura completa de la talanquera (BARRIER_ANGLE_OPEN).
     */
    void open();

    /**
     * @brief Comanda el cierre completo de la talanquera (BARRIER_ANGLE_CLOSED).
     */
    void close();

    /**
     * @brief Define un ángulo objetivo arbitrario para la barrera.
     * @param angle Ángulo en grados [0 - 180].
     */
    void setTargetAngle(uint8_t angle);

    /**
     * @brief Consulta el estado físico actual de la talanquera.
     */
    BarrierState getState() const;

    /**
     * @brief Indica si la talanquera está en desplazamiento o en asentamiento físico.
     */
    bool isMoving() const;

    /**
     * @brief Confirma si la talanquera alcanzó la apertura total y completó el asentamiento.
     */
    bool isFullyOpen() const;

    /**
     * @brief Confirma si la talanquera alcanzó el cierre total y completó el asentamiento.
     */
    bool isFullyClosed() const;

    /**
     * @brief Retorna el ángulo actual en grados.
     */
    uint8_t getCurrentAngle() const;

private:
    HardwareServo servo;
    uint8_t pin;
    uint8_t currentAngle;
    uint8_t targetAngle;
    uint32_t stepIntervalMs;
    uint32_t lastStepTime;
    uint32_t settleStartTime;
    bool settling;
    BarrierState state;

    void updateState();
    void attachIfNeeded();
};
