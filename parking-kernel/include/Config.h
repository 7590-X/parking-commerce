/**
 * @file Config.h
 * @brief Configuración global del sistema Parking Kernel (Arduino Uno / ATmega328P).
 *
 * Contiene la definición de pines de hardware, credenciales de conectividad,
 * parámetros de red MQTT, temporizadores no bloqueantes y constantes operativas.
 */

#pragma once
#include <Arduino.h>

// ============================================================================
// 1. ASIGNACIÓN FÍSICA DE PINES (ARDUINO UNO / ATmega328P)
// ============================================================================

// Comunicación Serie con ESP-01 (SoftwareSerial)
#define PIN_ESP_RX          10      ///< RX de Arduino <- TX de ESP-01 (3.3V)
#define PIN_ESP_TX          11      ///< TX de Arduino -> RX de ESP-01 (con divisor resistivo a 3.3V)

// Servomotor de la Talanquera (Hardware PWM Timer 1 - OC1A)
#define PIN_SERVO_BARRIER   9       ///< Pin PWM de control de la barrera vehicular

// Sensor Ultrasónico de Presencia (HC-SR04)
#define PIN_US_TRIG         6       ///< Disparo acústico (Trigger)
#define PIN_US_ECHO         7       ///< Recepción de eco (Echo)

// Semáforo LED Bicolor
#define PIN_LED_GREEN       2       ///< Indicador de acceso concedido / libre
#define PIN_LED_RED         3       ///< Indicador de alto / ocupado / alerta

// ============================================================================
// 2. PARÁMETROS DE RED WIFI Y BROKER MQTT
// ============================================================================

#define WIFI_SSID           "Cisco72164"
#define WIFI_PASS           "20AA4B48F5E6"
#define MQTT_BROKER_IP      "192.168.1.102"
#define MQTT_PORT           1883
#define MQTT_CLIENT_ID      "AR-UNO"

// ============================================================================
// 3. TÓPICOS MQTT (COMUNICACIÓN BIDIRECCIONAL)
// ============================================================================

// Comandos manuales o remotos recibidos desde el backend [Broker -> Arduino]
#define TOPIC_BARRIER_CMD    "kernel/arduino/barrier/cmd"

// Notificación de estado físico de la barrera [Arduino -> Broker]
#define TOPIC_BARRIER_STATE  "kernel/arduino/barrier/state"

// Eventos del sensor ultrasónico y seguridad [Arduino -> Broker]
#define TOPIC_SENSOR_EVENT   "kernel/arduino/event"

// Telemetría periódica de diagnóstico [Arduino -> Broker]
#define TOPIC_TELEMETRY      "kernel/arduino/telemetry"

// Solicitud de validación de espacio vehicular [Arduino -> Broker]
#define TOPIC_ENTRY_REQUEST  "kernel/arduino/request/in"

// Decisión de autorización emitida por el backend [Broker -> Arduino]
#define TOPIC_ENTRY_RESPONSE "kernel/arduino/response/in"

// ============================================================================
// 4. PAYLOADS ESTANDARIZADOS
// ============================================================================

#define PAYLOAD_CHECK_SPACE  "CHECK_SPACE"  ///< Solicitud de confirmación de espacio libre
#define PAYLOAD_ALLOW        "ALLOW"        ///< Acceso concedido
#define PAYLOAD_DENY         "DENY"         ///< Acceso denegado (estacionamiento lleno)

// ============================================================================
// 5. TEMPORIZADORES Y TIMEOUTS DE SISTEMA (MILISEGUNDOS)
// ============================================================================

#define AUTH_TIMEOUT_MS             10000UL ///< Tiempo máximo de espera de respuesta del broker
#define REJECTION_ALERT_MS          2500UL  ///< Duración de la alerta visual tras rechazo
#define MQTT_RECONNECT_INTERVAL_MS  5000UL  ///< Intervalo no bloqueante de reintento MQTT
#define TELEMETRY_INTERVAL_MS       15000UL ///< Frecuencia de emisión de telemetría de salud
#define WIFI_CHECK_INTERVAL_MS      10000UL ///< Período de sondeo de WiFi solo si MQTT está inactivo

// ============================================================================
// 6. PARÁMETROS DEL SENSOR ULTRASÓNICO (HC-SR04)
// ============================================================================

#define US_SAMPLE_INTERVAL_MS   60UL        ///< Período entre disparos acústicos (evita eco residual)
#define US_MAX_DISTANCE_CM      60          ///< Rango acústico para timeout de pulso (~3.5 ms max en pulseIn)
#define US_TIMEOUT_US           (US_MAX_DISTANCE_CM * 58UL) ///< Timeout acotado para pulseIn (~3.5 ms)
#define US_DETECT_THRESHOLD_CM  10          ///< Distancia límite para considerar detección (<= 10 cm)
#define US_DEBOUNCE_COUNT       3           ///< Muestras consecutivas requeridas para confirmar estado

// ============================================================================
// 7. PARÁMETROS DE LA TALANQUERA (HARDWARE SERVO)
// ============================================================================

#define BARRIER_ANGLE_CLOSED    5           ///< Ángulo seguro de reposo cerrado (evita tope mecánico)
#define BARRIER_ANGLE_OPEN      90          ///< Ángulo de apertura total
#define BARRIER_STEP_INTERVAL_MS 15UL       ///< Retardo por grado para suavidad de giro
#define BARRIER_SETTLE_MS       350UL       ///< Asentamiento físico antes de desacoplar PWM
#define BARRIER_AUTO_CLOSE_MS   3000UL      ///< Tiempo de cortesía con talanquera abierta tras cruce

// ============================================================================
// 8. TEMPORIZADORES DE SEÑALIZACIÓN LED
// ============================================================================

#define LED_BLINK_SLOW_MS       500UL       ///< Parpadeo lento (espera de autorización)
#define LED_BLINK_FAST_MS       150UL       ///< Parpadeo rápido (movimiento o rechazo)
