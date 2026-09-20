#include <BrokerLib.h>

namespace
{
    WiFiEspClient espClient;
    PubSubClient client(espClient);
    const char *mqttUsername = nullptr;
    const char *mqttPassword = nullptr;
    uint32_t lastReconnectAttempt = 0;
}

void setupMQTTClient(const char *broker, MQTTMessageHandler handler, const char *username, const char *password)
{
    mqttUsername = username;
    mqttPassword = password;
    client.setServer(broker, MQTT_PORT);
    client.setCallback(handler);
}

void updateMQTT(uint32_t now)
{
    // Si no hay WiFi, no intentar conectar MQTT
    if (WiFi.status() != WL_CONNECTED)
    {
        return;
    }

    if (client.connected())
    {
        client.loop();
        return;
    }

    // Reintento no bloqueante
    if (now - lastReconnectAttempt >= MQTT_RECONNECT_INTERVAL_MS)
    {
        lastReconnectAttempt = now;
        Serial.print(F("[MQTT] Intentando conexion no bloqueante... "));

        bool ok = false;
        if (mqttUsername != nullptr && strlen(mqttUsername) > 0)
        {
            ok = client.connect(MQTT_CLIENT_ID, mqttUsername, mqttPassword);
        }
        else
        {
            ok = client.connect(MQTT_CLIENT_ID);
        }

        if (ok)
        {
            Serial.println(F("Conectado!"));
            // Suscribirse automáticamente a los comandos de la barrera
            client.subscribe(TOPIC_BARRIER_CMD);
            client.subscribe(TOPIC_ENTRY_RESPONSE);
            Serial.print(F("[MQTT] Suscrito a: "));
            Serial.print(TOPIC_BARRIER_CMD);
            Serial.print(F(" y "));
            Serial.println(TOPIC_ENTRY_RESPONSE);
        }
        else
        {
            Serial.print(F("Fallo rc="));
            Serial.println(client.state());
        }
    }
}

bool isMQTTConnected()
{
    return client.connected();
}

bool sendMQTTMessage(const char *topic, const char *payload)
{
    if (!client.connected())
    {
        return false;
    }
    return client.publish(topic, payload);
}

bool subscribeMQTT(const char *topic)
{
    if (!client.connected())
    {
        return false;
    }
    return client.subscribe(topic);
}