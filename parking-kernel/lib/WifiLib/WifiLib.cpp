/**
 * @file WifiLib.cpp
 * @brief Implementación de la gestión WiFi con ESP-01.
 */

#include "WifiLib.h"

namespace
{
    SoftwareSerial espSerial(PIN_ESP_RX, PIN_ESP_TX);
    const char *storedSsid = nullptr;
    const char *storedPassword = nullptr;
    uint32_t lastWiFiCheck = 0;
    bool wifiConnected = false;
}

void setupWiFi(const char *ssid, const char *password)
{
    storedSsid = ssid;
    storedPassword = password;

    espSerial.begin(9600);
    WiFi.init(&espSerial);

    if (WiFi.status() == WL_NO_SHIELD)
    {
        Serial.println(F("[WiFi] ERROR: Modulo ESP-01 no responde."));
        wifiConnected = false;
        return;
    }

    Serial.print(F("[WiFi] Conectando a: "));
    Serial.println(ssid);

    WiFi.begin(ssid, password);

    // Espera acotada (máximo 10 segundos) durante el arranque inicial
    uint32_t startWait = millis();
    while (WiFi.status() != WL_CONNECTED && (millis() - startWait < 10000UL))
    {
        delay(500);
        Serial.print(F("."));
    }

    wifiConnected = (WiFi.status() == WL_CONNECTED);

    if (wifiConnected)
    {
        Serial.println(F("\n[WiFi] Conectado exitosamente!"));
        Serial.print(F("[WiFi] IP: "));
        Serial.println(WiFi.localIP());
    }
    else
    {
        Serial.println(F("\n[WiFi] Enlace no inmediato. Se reintentará en segundo plano."));
    }
}

bool isWiFiConnected()
{
    return wifiConnected;
}

void updateWiFi(uint32_t now, bool isNetworkActive)
{
    // Si la sesión MQTT está activa, el enlace WiFi está garantizado.
    // Omitimos AT+CIPSTATUS para no disparar espEmptyBuf() ni destruir paquetes MQTT entrantes.
    if (isNetworkActive)
    {
        wifiConnected = true;
        lastWiFiCheck = now;
        return;
    }

    // Sondeo de estado solo cuando no hay socket activo de capas superiores
    if (now - lastWiFiCheck < WIFI_CHECK_INTERVAL_MS)
    {
        return;
    }
    lastWiFiCheck = now;

    wifiConnected = (WiFi.status() == WL_CONNECTED);

    if (!wifiConnected && storedSsid != nullptr)
    {
        Serial.println(F("[WiFi] Reconectando en segundo plano..."));
        WiFi.begin(storedSsid, storedPassword);
        wifiConnected = (WiFi.status() == WL_CONNECTED);
        if (wifiConnected)
        {
            Serial.println(F("[WiFi] Reconectado exitosamente!"));
        }
    }
}