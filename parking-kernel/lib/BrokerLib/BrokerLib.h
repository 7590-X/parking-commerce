#pragma once
#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFiEsp.h>

using MQTTMessageHandler = void (*)(char *topic, byte *payload, unsigned int length);

void setupMQTTClient(const char *broker, const char *username, const char *password, MQTTMessageHandler handler);

void reconnectMQTTClient();

bool sendMQTTMessage(const char *topic, const char *payload);