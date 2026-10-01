/**
 * @file main.cpp
 * @brief Punto de entrada principal y scheduler cooperativo no bloqueante.
 *
 * Proyecto: Parking Kernel (Control de acceso vehicular para Arduino Uno y ESP8266).
 * Inicializa actuadores, conectividad WiFi/MQTT y despacha periódicamente las tareas.
 */

#include <Arduino.h>
#include <Config.h>
#include <WifiLib.h>
#include <BrokerLib.h>
#include <ParkingKernel.h>

// Instancia central del orquestador de parqueo
static ParkingKernel kernel;

/**
 * @brief Callback despachador de mensajes MQTT entrantes.
 */
static void onMQTTMessage(char *topic, byte *payload, unsigned int length)
{
    char messageBuffer[16];
    unsigned int copyLen = (length < sizeof(messageBuffer) - 1) ? length : sizeof(messageBuffer) - 1;
    memcpy(messageBuffer, payload, copyLen);
    messageBuffer[copyLen] = '\0';

    // Eliminar saltos de linea o espacios accidentales que puedan romper strcmp
    while (copyLen > 0 && (messageBuffer[copyLen - 1] == '\r' || messageBuffer[copyLen - 1] == '\n' || messageBuffer[copyLen - 1] == ' '))
    {
        copyLen--;
        messageBuffer[copyLen] = '\0';
    }

    Serial.print(F("[MQTT Rx] Topic: "));
    Serial.print(topic);
    Serial.print(F(" | Payload: "));
    Serial.println(messageBuffer);

    if (strcmp(topic, TOPIC_ENTRY_RESPONSE) == 0)
    {
        kernel.handleAuthResponse(messageBuffer);
    }
    else if (strcmp(topic, TOPIC_BARRIER_CMD) == 0)
    {
        kernel.handleCommand(messageBuffer);
    }
}

void setup()
{
    Serial.begin(9600);
    while (!Serial && millis() < 2000) { ; }

    Serial.println(F("\n======================================"));
    Serial.println(F("   PARKING KERNEL - ARDUINO UNO       "));
    Serial.println(F("======================================"));

    // 1. Inicializar lógica interna, hardware y actuadores primero
    kernel.begin();

    // 2. Inicializar conectividad de red y mensajería IoT
    setupWiFi(WIFI_SSID, WIFI_PASS);
    setupMQTTClient(MQTT_BROKER_IP, onMQTTMessage);
}

void loop()
{
    const uint32_t now = millis();

    // Scheduler cooperativo no bloqueante
    updateWiFi(now, isMQTTConnected());
    updateMQTT(now);
    kernel.update(now);
}