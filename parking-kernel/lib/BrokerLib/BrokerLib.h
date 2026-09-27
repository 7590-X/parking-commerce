/**
 * @file BrokerLib.h
 * @brief Capa de abstracción MQTT sobre PubSubClient para comunicación con el broker IoT.
 *
 * Gestiona conexiones no bloqueantes, publicación con telemetría espaciada,
 * secuenciación de suscripciones y enrutamiento de paquetes entrantes.
 */

#pragma once
#include <Arduino.h>
#include <PubSubClient.h>
#include <WiFiEsp.h>
#include <Config.h>

/**
 * @brief Firma de la función callback para despachar mensajes MQTT recibidos.
 */
using MQTTMessageHandler = void (*)(char *topic, byte *payload, unsigned int length);

/**
 * @brief Configura el servidor broker, credenciales opcionales y el callback de recepción.
 */
void setupMQTTClient(const char *broker, MQTTMessageHandler handler, const char *username = nullptr, const char *password = nullptr);

/**
 * @brief Tarea cooperativa para mantener la conexión MQTT viva y procesar eventos entrantes.
 * @param now Tiempo actual en milisegundos.
 */
void updateMQTT(uint32_t now);

/**
 * @brief Consulta si el cliente MQTT mantiene una sesión activa con el broker.
 */
bool isMQTTConnected();

/**
 * @brief Publica un mensaje de texto en el tópico indicado.
 * @param topic Tópico MQTT de destino.
 * @param payload Cadena de texto a publicar.
 * @return true si la publicación fue exitosa.
 */
bool sendMQTTMessage(const char *topic, const char *payload);

/**
 * @brief Se suscribe a un tópico MQTT.
 * @param topic Tópico al cual suscribirse.
 */
bool subscribeMQTT(const char *topic);