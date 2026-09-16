#include <Arduino.h>
#include <BrokerLib.h>
#include <WifiLib.h>

void onMQTTMessage(char *topic, byte *payload, unsigned int length)
{
  Serial.print("Mensaje recibido en el topic: ");
  Serial.println(topic);
  Serial.print("Payload: ");
  for (unsigned int i = 0; i < length; i++)
  {
    Serial.print((char)payload[i]);
  }
}

void setup()
{
  Serial.begin(9600);
  connectWiFi("Cisco72164", "20AA4B48F5E6");
  setupMQTTClient("192.168.1.100", onMQTTMessage);
}

void loop()
{
  sendMQTTMessage("kernel/arduino/talanquera","VLD");
}