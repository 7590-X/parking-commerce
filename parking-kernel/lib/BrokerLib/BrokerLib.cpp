#include <BrokerLib.h>

namespace
{
    WiFiEspClient espClient;
    PubSubClient client(espClient);
    const char *mqttUsername = nullptr;
    const char *mqttPassword = nullptr;
}

void setupMQTTClient(const char *broker, const char *username, const char *password, MQTTMessageHandler handler)
{
    mqttUsername = username;
    mqttPassword = password;
    client.setServer(broker, 1883);
    client.setCallback(handler);
}

void reconnectMQTTClient()
{
    while (!client.connected())
    {
        Serial.print("Connecting to MQTT...");
        if (client.connect("ARD", mqttUsername, mqttPassword))
        {
            Serial.println(" ¡Conectado!");
        }
        else
        {
            Serial.print("  MQTT connection failed [");
            Serial.print(client.state());
            Serial.println("], trying to connect again...");
            delay(3000);
        }
    }
}

bool sendMQTTMessage(const char *topic, const char *payload)
{
    return client.publish(topic, payload);
}