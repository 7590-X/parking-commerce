/**
 * @file WifiLib.h
 * @brief Capa de enlace y gestión de conectividad WiFi mediante módulo ESP-01 (ESP8266).
 *
 * Administra la conexión inicial y las reconexiones en segundo plano vía SoftwareSerial,
 * protegiendo el tráfico serie para no corromper paquetes de capas superiores.
 */

#pragma once
#include <Arduino.h>
#include <SoftwareSerial.h>
#include <WiFiEsp.h>
#include <Config.h>

/**
 * @brief Inicializa el módulo ESP-01 por SoftwareSerial y conecta a la red WiFi configurada.
 * @param ssid Nombre de la red WiFi (SSID).
 * @param password Clave WPA/WPA2 de la red.
 */
void setupWiFi(const char* ssid, const char* password);

/**
 * @brief Consulta el estado de conexión WiFi en memoria (sin tráfico serie).
 * @return true si la interfaz WiFi está conectada y con IP asignada.
 */
bool isWiFiConnected();

/**
 * @brief Supervisa el estado de red de manera no bloqueante.
 * @param now Tiempo actual en milisegundos.
 * @param isNetworkActive Si es true (ej. sesión MQTT activa), omite consultas AT (AT+CIPSTATUS)
 *                        para evitar vaciar buffers serie de recepción.
 */
void updateWiFi(uint32_t now, bool isNetworkActive = false);