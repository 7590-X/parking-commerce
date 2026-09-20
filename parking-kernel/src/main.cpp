#include <Arduino.h>
#include <Config.h>
#include <WifiLib.h>
#include <BrokerLib.h>
#include <ParkingKernel.h>

ParkingKernel kernel;

void onMQTTMessage(char *topic, byte *payload, unsigned int length)
{
    char messageBuffer[16];
    unsigned int copyLen = (length < sizeof(messageBuffer) - 1) ? length : sizeof(messageBuffer) - 1;
    memcpy(messageBuffer, payload, copyLen);
    messageBuffer[copyLen] = '\0';

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
    while (!Serial && millis() < 2000) { ; } // Espera opcional en puertos nativos

    Serial.println(F("\n======================================"));
    Serial.println(F("   PARKING KERNEL - ARDUINO UNO       "));
    Serial.println(F("======================================"));

    // 1. Inicializar lógica y actuadores primero (seguridad de hardware)
    kernel.begin();

    // 2. Inicializar conectividad
    setupWiFi(WIFI_SSID, WIFI_PASS);
    setupMQTTClient(MQTT_BROKER_IP, onMQTTMessage);
}

void loop()
{
    uint32_t now = millis();

    // Tareas cooperativas no bloqueantes
    updateWiFi(now);
    updateMQTT(now);
    kernel.update(now);
}