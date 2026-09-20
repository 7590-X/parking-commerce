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
│   ├── WiFiEsp/                 # Driver WiFiEsp local parcheado para compatibilidad AT 1.3.0
│   │   ├── src/
│   │   │   ├── WiFiEsp.h / .cpp
│   │   │   ├── WiFiEspClient.h / .cpp
│   │   │   ├── WiFiEspServer.h / .cpp
│   │   │   ├── WiFiEspUdp.h / .cpp
│   │   │   └── utility/
│   │   │       ├── EspDrv.h / .cpp      <-- [ARCHIVO PARCHEADO]
│   │   │       ├── RingBuffer.h / .cpp
│   │   │       └── debug.h
│   │   ├── library.properties
│   │   └── README.md
│   └── WifiLib/                 # Gestor de conexión WiFi con caché en memoria y reconexión en segundo plano
│       ├── WifiLib.cpp
│       └── WifiLib.h
├── src/
│   └── main.cpp                 # Punto de entrada (setup) y scheduler cooperativo no bloqueante (loop)
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
| **`Ultrasonic`** | Lectura precisa con pulso de 10µs y medición acotada (`US_TIMEOUT_US = ~5.8ms`), integrando filtro anti-rebote configurable. |
| **`Indicators`** | Semáforo bicolor con patrones luminosos: Libre (Verde), Ocupado (Rojo), En movimiento (Parpadeo rápido) y Espera (Parpadeo lento). |
| **`WiFiEsp`** | Driver local para comunicación AT con el chip ESP8266. Incluye corrección crítica en el parser de tramas entrantes `+IPD`. |

---

## Documentación Técnica del Parche en `WiFiEsp`

### 1. Diagnóstico del Problema Original
Al conectar el Arduino Uno al broker MQTT a través del ESP-01, se observaba una desconexión en bucle infinito en el monitor serie:

```text
[MQTT] Intentando conexion no bloqueante... [WiFiEsp] Connecting to 192.168.1.102
Conectado!
[MQTT] Suscrito a: kernel/arduino/barrier/cmd y kernel/arduino/response
[WiFiEsp] TIMEOUT: 5
[WiFiEsp] TIMEOUT: 2
[WiFiEsp] Disconnecting  3
```

#### Causa Raíz
1. **Incompatibilidad de firmware AT**: El ESP-01 reporta versión `SDK version: 1.3.0`. El comando `AT+CIPDINFO=1` (que instruye al ESP a adjuntar la IP y puerto remoto en las tramas `+IPD`) no existe en el SDK 1.3.0 (fue introducido por Espressif a partir del SDK 1.5.0). Al inicializar, el ESP responde `ERROR` a dicho comando, permaneciendo en modo estándar `AT+CIPDINFO=0`.
2. **Formato de trama emitida por el ESP**:
   - Formato estándar emitido: `+IPD,<conn_id>,<len>:<payload>`
   - Formato extendido esperado por la librería: `+IPD,<conn_id>,<len>,"<remote_ip>",<port>:<payload>`
3. **Corrupción del payload por `parseInt()`**:
   En la versión oficial de `WiFiEsp 2.2.2`, la función `EspDrv::availData` leía `<len>` y asumía incondicionalmente que el siguiente bloque correspondía a la IP remota entre comillas.
   Al llegar el delimitador `:` inmediatamente después de la longitud (ej. `+IPD,3,5:\x90\x03\x00\x01\x00`), el parser ejecutaba llamadas a `parseInt()` sobre los datos binarios del paquete MQTT (`SUBACK` de 5 bytes).
   Como resultado:
   - Los 5 bytes del paquete eran consumidos y descartados por `parseInt()` en su búsqueda fallida de dígitos numéricos.
   - La función `availData` reportaba que había 5 bytes disponibles (`_bufPos = 5`), pero el buffer serie ya estaba vacío.
   - Cuando `WiFiEspClient::read()` llamaba a `EspDrv::getData()`, la espera agotaba el tiempo límite de 2 segundos arrojando `[WiFiEsp] TIMEOUT: 5`.
   - Al no confirmarse la suscripción, `PubSubClient` cerraba el socket (`Disconnecting 3`) y repetía el ciclo indefinidamente.

---

### 2. Modificación Implementada

Se modificó la función `EspDrv::availData` en el archivo local:
📁 `lib/WiFiEsp/src/utility/EspDrv.cpp` (Líneas 674 a 696)

#### Código Original (`WiFiEsp 2.2.2` oficial):
```cpp
// Asume ciegamente que tras <len> viene una coma y la IP remota entre comillas:
_connId = espSerial->parseInt();    // <ID>
espSerial->read();                  // ,
_bufPos = espSerial->parseInt();    // <len>
espSerial->read();                  // "  <-- Si el firmware envió ':', consume los datos
_remoteIp[0] = espSerial->parseInt();    // <remote IP> (devora el payload MQTT)
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
```

#### Código Corregido (Parche de compatibilidad universal):
```cpp
_connId = espSerial->parseInt();    // <ID>
espSerial->read();                  // ,
_bufPos = espSerial->parseInt();    // <len>

// Evalúa si el delimitador posterior es ':' (estándar) o ',' (con CIPDINFO)
char c = espSerial->read();
if (c == ',')
{
    // Solo si hay coma, se procesa la información de IP y puerto remoto
    _remoteIp[0] = espSerial->parseInt();
    espSerial->read();                  // .
    _remoteIp[1] = espSerial->parseInt();
    espSerial->read();                  // .
    _remoteIp[2] = espSerial->parseInt();
    espSerial->read();                  // .
    _remoteIp[3] = espSerial->parseInt();
    espSerial->read();                  // "
    espSerial->read();                  // ,
    _remotePort = espSerial->parseInt();
    espSerial->read();                  // :
}
// Si c == ':', la cabecera termina aquí. El primer byte del payload queda intacto en el buffer serie.
```

#### Beneficio
- **Compatibilidad total**: Funciona transparentemente tanto con firmwares ESP8266 antiguos (SDK 1.3.0, 1.4.0) como con versiones modernas (SDK 1.5.0+, AT 1.7.x, AT 2.x).
- **Integridad de datos**: Los paquetes binarios de MQTT (`CONNACK`, `SUBACK`, `PUBLISH`) se conservan sin corrupción ni bytes devorados.

---

### 3. Optimizaciones Complementarias en el Kernel

Junto con el parche de la librería, se efectuaron tres mejoras esenciales para garantizar estabilidad a largo plazo:

1. **Caché de Estado WiFi en Memoria (`lib/WifiLib/WifiLib.cpp`)**:
   - *Problema previo*: `BrokerLib.cpp` llamaba a `WiFi.status()` en cada ciclo de `loop()`. Dicha función enviaba `AT+CIPSTATUS` al ESP y llamaba a `espEmptyBuf()`, vaciando el buffer serie hasta 100 veces por segundo y destruyendo cualquier paquete entrante.
   - *Solución*: Se introdujo la variable booleana `wifiConnected`. `isWiFiConnected()` responde en $O(1)$ sin tráfico serie, y la consulta física al ESP mediante `WiFi.status()` solo ocurre cada 10 segundos en `updateWiFi()`.

2. **Espaciado en Suscripciones MQTT (`lib/BrokerLib/BrokerLib.cpp`)**:
   - Al conectarse al broker, se incluyeron llamadas a `client.loop()` y una pausa controlada de 50ms entre `client.subscribe(TOPIC_BARRIER_CMD)` y `client.subscribe(TOPIC_ENTRY_RESPONSE)`. Esto asegura que el `SUBACK` del primer tópico sea recibido y procesado por el ESP antes de enviar el segundo comando `AT+CIPSEND`.

3. **Ampliación del Buffer RX de `SoftwareSerial` (`platformio.ini`)**:
   - Se configuró la bandera `-D_SS_MAX_RX_BUFF=128`. El buffer de recepción por software se amplió de 64 a 128 bytes, previniendo desbordamientos mientras el microcontrolador atiende las interrupciones del servomotor o la lectura del sensor HC-SR04.

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
