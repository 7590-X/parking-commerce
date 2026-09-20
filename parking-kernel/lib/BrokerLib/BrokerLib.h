#pragma once
#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFiEsp.h>
#include <Config.h>

using MQTTMessageHandler = void (*)(char *topic, byte *payload, unsigned int length);

void setupMQTTClient(const char *broker, MQTTMessageHandler handler, const char *username = nullptr, const char *password = nullptr);

// Mantiene la conexión MQTT y procesa los paquetes entrantes sin bloquear
void updateMQTT(uint32_t now);

// Estado de la conexión MQTT
bool isMQTTConnected();

// Publica un mensaje en un tópico
bool sendMQTTMessage(const char *topic, const char *payload);

// Suscripción a un tópico MQTT
bool subscribeMQTT(const char *topic);