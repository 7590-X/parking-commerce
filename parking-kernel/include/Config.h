#pragma once
#include <Arduino.h>

// ==========================================
// CONFIGURACIÓN DE RED Y MQTT
// ==========================================
#define WIFI_SSID           "Cisco72164"
#define WIFI_PASS           "20AA4B48F5E6"
#define MQTT_BROKER_IP      "192.168.1.102"
#define MQTT_PORT           1883
#define MQTT_CLIENT_ID      "AR-UNO"

// ==========================================
// TÓPICOS MQTT (COMUNICACIÓN CON EL BROKER)
// ==========================================

// [Broker -> Arduino] Comandos directos/manuales para la talanquera.
// Payloads soportados: "OPEN", "CLOSE", "AUTO_ON", "AUTO_OFF"
#define TOPIC_BARRIER_CMD    "kernel/arduino/barrier/cmd"

// [Arduino -> Broker] Notifica el estado físico y movimiento de la talanquera en tiempo real.
// Payloads emitidos: "OPENING", "OPEN", "CLOSING", "CLOSED"
#define TOPIC_BARRIER_STATE  "kernel/arduino/barrier/state"

// [Arduino -> Broker] Publica eventos clave del sensor de presencia y alertas de seguridad.
// Payloads emitidos: "VEHICLE_ARRIVED", "VEHICLE_CLEARED", "REQUEST_CANCELLED",
//                    "ACCESS_DENIED_NO_SPACE", "OBSTACLE_DETECTED_REOPENING", "AUTH_TIMEOUT"
#define TOPIC_SENSOR_EVENT   "kernel/arduino/event"

// [Arduino -> Broker] Telemetría periódica de diagnóstico (salud del dispositivo, distancia, ángulo).
// Formato emitido: "dist=<cm>,state=<estado_fsm>,ang=<angulo_servo>" (cada 15s)
#define TOPIC_TELEMETRY      "kernel/arduino/telemetry"

// [Arduino -> Broker] Canal de consulta de acceso.
// Se emite cuando un auto se aproxima a la entrada para verificar si hay plazas libres.
// Payload emitido: "CHECK_SPACE"
#define TOPIC_ENTRY_REQUEST  "kernel/arduino/request/in"

// [Broker -> Arduino] Canal de respuesta con la decisión de acceso tomada por el backend/broker.
// Payloads esperados: "ALLOW", "DENY"
#define TOPIC_ENTRY_RESPONSE "kernel/arduino/response/in"

// Mensajes y Comandos esperados
#define PAYLOAD_CHECK_SPACE  "CHECK_SPACE"
#define PAYLOAD_ALLOW        "ALLOW"
#define PAYLOAD_DENY         "DENY"

// Tiempos de espera de autorización (ms)
#define AUTH_TIMEOUT_MS             10000UL
#define REJECTION_ALERT_MS          2500UL

// Intervalo de reconexión MQTT no bloqueante (ms)
#define MQTT_RECONNECT_INTERVAL_MS  5000UL
#define TELEMETRY_INTERVAL_MS       15000UL

// ==========================================
// ASIGNACIÓN DE PINES (ARDUINO UNO)
// ==========================================
// Pines Serial ESP-01 (SoftwareSerial)
#define PIN_ESP_RX          10
#define PIN_ESP_TX          11

// Servomotor Talanquera
#define PIN_SERVO_BARRIER   9

// Sensor Ultrasónico de Entrada (HC-SR04)
#define PIN_US_TRIG         6
#define PIN_US_ECHO         7

// Señalización LED de Entrada
#define PIN_LED_GREEN       2
#define PIN_LED_RED         3

// ==========================================
// PARÁMETROS DE SENSORES Y ACTUADORES
// ==========================================
// Sensor Ultrasónico
#define US_SAMPLE_INTERVAL_MS   60UL    // Tiempo entre lecturas (evita eco residual)
#define US_MAX_DISTANCE_CM      100     // Rango máximo relevante (1 metro)
// Timeout acotado para pulseIn: 100 cm * 58 us/cm = 5800 us (~5.8 ms max de espera)
#define US_TIMEOUT_US           (US_MAX_DISTANCE_CM * 58UL)
#define US_DETECT_THRESHOLD_CM  20      // Presencia detectada si distancia <= 20 cm
#define US_DEBOUNCE_COUNT       2       // Lecturas consecutivas para confirmar estado

// Servomotor Talanquera
#define BARRIER_ANGLE_CLOSED    0
#define BARRIER_ANGLE_OPEN      90
#define BARRIER_STEP_INTERVAL_MS 15UL   // Milisegundos por grado (suavidad de giro)
#define BARRIER_AUTO_CLOSE_MS   3000UL  // Tiempo abierto antes de cerrar tras despeje

// Señalización LED
#define LED_BLINK_SLOW_MS       500UL
#define LED_BLINK_FAST_MS       150UL
