#include <WifiLib.h>

void connectWiFi(const char *ssid, const char *password)
{
    SoftwareSerial espSerial(10, 11);
    espSerial.begin(9600);
    espSerial.println("AT+RST");
    delay(2000);
    espSerial.println("AT+CWMODE=1");
    delay(1000);

    WiFi.init(&espSerial);

    while (WiFi.status() == WL_NO_SHIELD)
    {
        Serial.println("Error: ESP-01 no responde.");
        delay(1000);
    }

    WiFi.disconnect();
    delay(1000);

    Serial.print("Connecting to WiFi... ");
    Serial.println(ssid);
    WiFi.begin(ssid, password);

    while (WiFi.status() != WL_CONNECTED)
    {
        delay(1000);
        Serial.print(".");
    }

    Serial.println("\n--- WiFi Conectado ---");
    Serial.print("Dirección IP: ");
    Serial.println(WiFi.localIP());
}