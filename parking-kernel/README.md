# Parking Kernel - Arduino Uno & ESP8266

Firmware embebido para el sistema de control de acceso vehicular de estacionamiento. Gestiona de manera cooperativa y no bloqueante la barrera electromecánica (servo), sensor ultrasónico de presencia (HC-SR04), semáforo visual (LEDs de estado) y la comunicación bidireccional IoT vía MQTT a través de un módulo WiFi ESP-01 (ESP8266).

---

## Estructura del Proyecto

El proyecto está organizado siguiendo el estándar modular de PlatformIO, desacoplando cada subsistema de hardware y comunicación en librerías independientes (`lib/`):

```text
parking-kernel/
├── include/
│   └── Config.h                 # Constantes globales: pines, credenciales WiFi/MQTT, tópicos y temporizadores
├── lib/
│   ├── Barrier/                 # Controlador de la talanquera (servomotor)
│   │   ├── BarrierServo.cpp
│   │   └── BarrierServo.h
│   ├── BrokerLib/               # Cliente MQTT no bloqueante (PubSubClient) y gestión de eventos
│   │   ├── BrokerLib.cpp
│   │   └── BrokerLib.h
│   ├── Indicators/              # Gestión de señalización visual (LEDs verde y rojo)
│   │   ├── LedIndicator.cpp
│   │   └── LedIndicator.h
│   ├── ParkingKernel/           # Máquina de estados finitos (FSM) de control de acceso
│   │   ├── ParkingKernel.cpp
│   │   └── ParkingKernel.h
│   ├── Ultrasonic/              # Sensor ultrasónico HC-SR04 con anti-rebote y tiempos acotados
│   │   ├── UltrasonicSensor.cpp
│   │   └── UltrasonicSensor.h
│   ├── WiFiEsp/                 # Driver WiFiEsp local optimizado y parcheado para AT 1.3.0 y MQTT
│   │   ├── src/
│   │   │   ├── WiFiEsp.h / .cpp
│   │   │   ├── WiFiEspClient.h / .cpp   <-- [PARCHEADO: status() O(1) sin sondeo AT, eliminación de delay(4000)]
│   │   │   ├── WiFiEspServer.h / .cpp
│   │   │   ├── WiFiEspUdp.h / .cpp
│   │   │   └── utility/
│   │   │       ├── EspDrv.h / .cpp      <-- [PARCHEADO: parser +IPD universal, timeout 50ms, buffers reducidos]
│   │   │       ├── RingBuffer.h / .cpp
│   │   │       └── debug.h
│   │   ├── library.properties
│   │   └── README.md
│   └── WifiLib/                 # Gestor de conexión WiFi con caché en memoria y reconexión en segundo plano
│       ├── WifiLib.cpp
│       └── WifiLib.h
├── src/
│   └── main.cpp                 # Punto de entrada (setup), sanitización MQTT y scheduler cooperativo (loop)
├── test/                        # Pruebas unitarias
├── platformio.ini               # Configuración de compilación de PlatformIO, flags y dependencias
└── README.md                    # Documentación del proyecto
```

### Descripción de Módulos

| Módulo | Responsabilidad |
|---|---|
| **`Config.h`** | Mapeo físico de pines del ATmega328P, tópicos MQTT, credenciales de red, intervalos de muestreo y ángulos de giro. |
| **`ParkingKernel`** | Orquestador central del sistema. Implementa la FSM (`IDLE`, `VEHICLE_WAIT_AUTH`, `OPENING`, `OPEN_WAIT_PASS`, `CLEARING_DELAY`, `CLOSING`, `ACCESS_DENIED`). |
| **`BrokerLib`** | Capa de abstracción MQTT sobre `PubSubClient`. Gestiona reconexiones automáticas, secuenciación de suscripciones y emisión de telemetría. |
| **`WifiLib`** | Capa de enlace de red para el ESP-01 vía `SoftwareSerial`. Monitorea el estado mediante variables cacheadas sin saturar el canal serie. |
| **`Barrier`** | Control de movimiento angular suave del servomotor grado a grado sin retardos bloqueantes (`delay`). |
| **`Ultrasonic`** | Lectura precisa con pulso de 10µs y medición acotada (`US_TIMEOUT_US = ~3.5ms`), integrando filtro anti-rebote configurable. |
| **`Indicators`** | Semáforo bicolor con patrones luminosos: Libre (Verde), Ocupado (Rojo), En movimiento (Parpadeo rápido) y Espera (Parpadeo lento). |
| **`WiFiEsp`** | Driver local para comunicación AT con el chip ESP8266. Incluye corrección del parser `+IPD`, desacople de sondeo AT destructivo y optimización de memoria. |

---

## Documentación Técnica de Parches y Optimizaciones en `WiFiEsp`

El driver `WiFiEsp` oficial presentaba limitaciones arquitectónicas severas al operar como puente entre `PubSubClient` (sesión TCP persistente) y un ESP8266 por `SoftwareSerial` en un microcontrolador de recursos mínimos (ATmega328P). Se implementaron dos parches críticos y una serie de optimizaciones de memoria.

---

### Parche 1: Compatibilidad de Cabeceras `+IPD` en SDK 1.3.0 (Parser Universal)

#### Diagnóstico Original
Al inicializar la sesión MQTT, el sistema entraba en un bucle infinito de desconexiones:
```text
[MQTT] Intentando conexion no bloqueante... [WiFiEsp] Connecting to 192.168.1.102
Conectado!
[MQTT] Suscrito a: kernel/arduino/barrier/cmd y kernel/arduino/response
[WiFiEsp] TIMEOUT: 5
[WiFiEsp] TIMEOUT: 2
[WiFiEsp] Disconnecting  3
```

#### Causa Raíz
* **Firmware AT legado:** El ESP-01 ejecuta `SDK version: 1.3.0`. El comando `AT+CIPDINFO=1` (que incluye la IP y puerto remoto en la cabecera `+IPD`) no existe en dicha versión (se añadió en SDK 1.5.0).
* **Consumo erróneo del payload por `parseInt()`:** En la versión oficial de `WiFiEsp 2.2.2`, `EspDrv::availData` asumía incondicionalmente una coma y la IP remota entre comillas tras la longitud. Al recibir un delimitador dos puntos `:` estándar (ej. `+IPD,0,5:\x90\x03...`), `parseInt()` intentaba buscar números sobre el payload binario MQTT (`SUBACK`), devorando los datos y dejando el buffer vacío, provocando el `TIMEOUT: 5` al intentar leer.

#### Solución (`lib/WiFiEsp/src/utility/EspDrv.cpp`)
Se adaptó el parser para evaluar dinámicamente el delimitador:
```cpp
_connId = espSerial->parseInt();    // <ID>
espSerial->read();                  // ,
_bufPos = espSerial->parseInt();    // <len>

char c = espSerial->read();         // Evalúa delimitador
if (c == ',')
{
    // Solo si hay coma se extrae la información extendida de IP y puerto
    _remoteIp[0] = espSerial->parseInt();
    espSerial->read();                  // .
    _remoteIp[1] = espSerial->parseInt();
    espSerial->read();                  // .
    _remoteIp[2] = espSerial->parseInt();
    espSerial->read();                  // .
    _remoteIp[3] = espSerial->parseInt();
    espSerial->read();                  // "
    espSerial->read();                  // ,
    _remotePort = espSerial->parseInt();     // <remote port>
    espSerial->read();                  // :
}
// Si c == ':', la cabecera concluye y el payload MQTT queda intacto en el buffer serie.
```

---

### Parche 2: Desacoplamiento del Sondeo AT Destructivo en `WiFiEspClient::status()`

#### Diagnóstico del Problema
A pesar de estar conectado al broker:
1. **Llegada tardía de mensajes:** Los mensajes del broker (`ALLOW`/`DENY`) se perdían reiteradamente y solo ingresaban tras el 3.er intento.
2. **Desconexiones intermitentes del ESP:** El módulo ESP-01 se desconectaba o reiniciaba aleatoriamente durante la operación normal.

#### Causa Raíz
1. **Vaciado destructivo del buffer serie (`espEmptyBuf`):**  
   En cada iteración del `loop()`, `PubSubClient::loop()` invocaba `connected()`, la cual llamaba a `WiFiEspClient::status()`. En el driver original:
   ```cpp
   // CÓDIGO ORIGINAL DEFECTUOSO:
   uint8_t WiFiEspClient::status() {
       if (_sock == 255) return CLOSED;
       if (EspDrv::availData(_sock)) return ESTABLISHED;
       if (EspDrv::getClientState(_sock)) return ESTABLISHED; // <-- Causa raíz
       ...
   }
   ```
   Cuando no había datos pendientes en ese microsegundo exacto, llamaba a `getClientState()`, la cual transmitía `AT+CIPSTATUS` por `SoftwareSerial` y ejecutaba internamente **`espEmptyBuf()`**. Dicha función **borraba y purgaba el buffer de recepción del Arduino**. Si la respuesta del broker (`+IPD`) acababa de llegar, era **eliminada antes de que el callback pudiera procesarla**.
2. **Colisiones y cierre espurio del socket:**  
   Si la trama `+IPD` llegaba mientras `EspDrv` esperaba la respuesta de `AT+CIPSTATUS`, el parser se desincronizaba, `getClientState()` retornaba `false`, y `status()` cerraba y liberaba el socket erróneamente (`_sock = 255`), forzando una desconexión total.
3. **Saturación del procesador del ESP8266:**  
   Enviar decenas de comandos `AT+CIPSTATUS` por segundo a 9600 baudios inundaba la cola UART del ESP-01, provocando reinicios por **Watchdog Timer (WDT)**.

#### Solución (`lib/WiFiEsp/src/WiFiEspClient.cpp`)
Se desacopló `status()` del canal serie, consultando directamente el descriptor de socket en memoria en tiempo constante $O(1)$:
```cpp
uint8_t WiFiEspClient::status()
{
    if (_sock == 255 || WiFiEspClass::_state[_sock] == NA_STATE)
    {
        _sock = 255;
        return CLOSED;
    }

    return ESTABLISHED;
}
```
* **Detección natural de desconexión:** Si el broker cierra la conexión, `write()` falla en la siguiente emisión o keepalive ping, invocando `stop()`, lo que actualiza `_sock = 255` sin necesidad de sondeos AT destructivos.
* **Eliminación de bloqueo de 4 segundos:** En `WiFiEspClient::write()`, se eliminó la llamada `delay(4000);` que congelaba el microcontrolador ante cualquier fallo transitorio de escritura.

---

### Parche 3: Acotamiento de Timeout de Lectura en `EspDrv`

#### Causa
Arduino `Stream` utiliza un tiempo de espera predeterminado de **1,000 ms** (`setTimeout(1000)`). Si `EspDrv::availData` encontraba bytes no reconocidos, el microcontrolador se bloqueaba durante 1 segundo completo.

#### Solución (`lib/WiFiEsp/src/utility/EspDrv.cpp`)
En `EspDrv::wifiDriverInit`, se fijó un timeout estricto de **50 ms**:
```cpp
EspDrv::espSerial = espSerial;
espSerial->setTimeout(50); // Evita bloqueos de 1 segundo en métodos find() y parseInt()
```

---

### Parche 4: Optimización de Huella de Memoria (SRAM & Pila)

Para asegurar estabilidad en el microcontrolador ATmega328P (2 KB de RAM):

1. **Reducción de matrices de escaneo (`lib/WiFiEsp/src/utility/EspDrv.h`):**
   * Se redujo `WL_NETWORKS_LIST_MAXNUM` de 10 a 2 y `CMD_BUFFER_SIZE` de 200 a 80 bytes. Esto ahorra memoria de pila y libera espacio estático.
2. **Dimensionamiento del Buffer de Paquetes MQTT (`platformio.ini`):**
   * Se configuró `-DMQTT_MAX_PACKET_SIZE=128` (por defecto 256 bytes), ahorrando 128 bytes en el heap dinámico.
   * Se fijó `-D_ESPLOGLEVEL_=1`, reduciendo el uso de Flash de **22,872 bytes a 21,776 bytes** (ahorro de más de 1 KB).
3. **Sanitización de Payloads en `src/main.cpp`:**
   * En `onMQTTMessage`, se implementó el recorte de caracteres de fin de línea (`\r`, `\n`) y espacios residuales para garantizar que las comparaciones de texto (`strcmp`) coincidan de manera instantánea y robusta.

---

## Esquema de Conexiones de Hardware (Arduino Uno)

| Componente | Pin Arduino | Función / Descripción |
|---|---|---|
| **ESP-01 (ESP8266)** | Pin 10 (RX Arduino) | Conectar al TX del ESP-01 (Nivel lógico 3.3V) |
| | Pin 11 (TX Arduino) | Conectar al RX del ESP-01 (mediante divisor de voltaje 5V a 3.3V) |
| | VCC / CH_PD (EN) | Alimentación externa de 3.3V (hasta 300mA pico; evitar el pin 3.3V del Uno) |
| | GND | Tierra compartida (común con Arduino) |
| **Servomotor** | Pin 9 (PWM) | Señal de control de la talanquera |
| **Sensor HC-SR04** | Pin 6 | Trigger (Disparo de pulso 10µs) |
| | Pin 7 | Echo (Lectura de eco con timeout de 5.8ms) |
| **Semáforo LED** | Pin 2 | LED Verde (Acceso concedido / Libre) |
| | Pin 3 | LED Rojo (Acceso denegado / Ocupado) |

---

## Compilación y Despliegue

El proyecto se gestiona mediante PlatformIO CLI o la extensión oficial para VS Code:

### Compilar el firmware:
```bash
pio run
```

### Subir al microcontrolador:
```bash
pio run --target upload
```

### Abrir monitor serie (9600 baudios):
```bash
pio device monitor -b 9600
```
