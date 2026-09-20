#include <WifiLib.h>

namespace
{
    // SoftwareSerial persistente para evitar la destrucción del objeto en la pila
    SoftwareSerial espSerial(PIN_ESP_RX, PIN_ESP_TX);
    const char *storedSsid = nullptr;
    const char *storedPassword = nullptr;
    uint32_t lastWiFiCheck = 0;
    const uint32_t WIFI_CHECK_INTERVAL_MS = 10000UL;
}

void setupWiFi(const char *ssid, const char *password)
{
    storedSsid = ssid;
    storedPassword = password;

    espSerial.begin(9600);
    espSerial.println("AT+RST");
    espSerial.println("AT+CWMODE=1");
    WiFi.init(&espSerial);

    if (WiFi.status() == WL_NO_SHIELD)
    {
        Serial.println(F("[WiFi] ERROR: Modulo ESP-01 no responde."));
        return;
    }

    Serial.print(F("[WiFi] Conectando a: "));
    Serial.println(ssid);

    // Intento inicial
    WiFi.begin(ssid, password);

    // Espera acotada (máx 10 segundos) durante setup
    uint32_t startWait = millis();
    while (WiFi.status() != WL_CONNECTED && (millis() - startWait < 10000UL))
    {
        delay(500);
        Serial.print(F("."));
    }

    if (WiFi.status() == WL_CONNECTED)
    {
        Serial.println(F("\n[WiFi] Conectado exitosamente!"));
        Serial.print(F("[WiFi] IP: "));
        Serial.println(WiFi.localIP());
    }
    else
    {
        Serial.println(F("\n[WiFi] No se pudo conectar de inmediato. Se reintentará en segundo plano."));
    }
}

bool isWiFiConnected()
{
    return WiFi.status() == WL_CONNECTED;
}

void updateWiFi(uint32_t now)
{
    // Verificación periódica sin bloquear
    if (now - lastWiFiCheck < WIFI_CHECK_INTERVAL_MS)
    {
        return;
    }
    lastWiFiCheck = now;

    if (WiFi.status() != WL_CONNECTED && storedSsid != nullptr)
    {
        Serial.println(F("[WiFi] Reconectando en segundo plano..."));
        WiFi.begin(storedSsid, storedPassword);
    }
}