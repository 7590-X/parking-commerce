#pragma once
#include <Arduino.h>
#include <SoftwareSerial.h>
#include <WiFiEsp.h>
#include <Config.h>

// Inicializa el hardware WiFi (ESP-01) y realiza la conexión inicial
void setupWiFi(const char* ssid, const char* password);

// Verifica el estado de conexión WiFi
bool isWiFiConnected();

// Mantiene el estado de conexión WiFi de forma no bloqueante
void updateWiFi(uint32_t now);