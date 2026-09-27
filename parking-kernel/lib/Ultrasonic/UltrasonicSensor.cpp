/**
 * @file UltrasonicSensor.cpp
 * @brief Implementación de la adquisición de distancia con HC-SR04 y filtro anti-rebote.
 */

#include "UltrasonicSensor.h"

// Constante física: velocidad del sonido ~343 m/s a 20°C (29.1 us/cm de ida, 58.2 us ida y vuelta)
static constexpr uint32_t US_ROUNDTRIP_CM = 58UL;
static constexpr uint16_t DISTANCE_OUT_OF_RANGE = 999;
static constexpr uint16_t MIN_VALID_DISTANCE_CM = 3;

UltrasonicSensor::UltrasonicSensor()
    : trigPin(255),
      echoPin(255),
      thresholdCm(US_DETECT_THRESHOLD_CM),
      sampleIntervalMs(US_SAMPLE_INTERVAL_MS),
      timeoutUs(US_TIMEOUT_US),
      lastSampleTime(0),
      lastDistanceCm(DISTANCE_OUT_OF_RANGE),
      vehiclePresent(false),
      debounceCounter(0),
      arrivedEvent(false),
      clearedEvent(false)
{
}

void UltrasonicSensor::begin(uint8_t triggerPin, uint8_t echoPinNum,
                             uint16_t detectThresholdCm,
                             uint32_t sampleInterval,
                             uint32_t timeoutMicroseconds)
{
    trigPin = triggerPin;
    echoPin = echoPinNum;
    thresholdCm = detectThresholdCm;
    sampleIntervalMs = sampleInterval;
    timeoutUs = timeoutMicroseconds;

    pinMode(trigPin, OUTPUT);
    pinMode(echoPin, INPUT);
    digitalWrite(trigPin, LOW);

    lastSampleTime = 0;
    lastDistanceCm = DISTANCE_OUT_OF_RANGE;
    vehiclePresent = false;
    debounceCounter = 0;
    arrivedEvent = false;
    clearedEvent = false;
}

uint16_t UltrasonicSensor::readDistanceOnce()
{
    if (trigPin == 255 || echoPin == 255)
    {
        return DISTANCE_OUT_OF_RANGE;
    }

    // Pulso de disparo acústico de 10 microsegundos
    digitalWrite(trigPin, LOW);
    delayMicroseconds(2);
    digitalWrite(trigPin, HIGH);
    delayMicroseconds(10);
    digitalWrite(trigPin, LOW);

    // Medición con timeout acotado (máximo ~5.8 ms para 1 metro)
    // Previene cualquier bloqueo de CPU en caso de ausencia de eco
    unsigned long duration = pulseIn(echoPin, HIGH, timeoutUs);

    if (duration == 0)
    {
        return DISTANCE_OUT_OF_RANGE;
    }

    return (uint16_t)(duration / US_ROUNDTRIP_CM);
}

void UltrasonicSensor::update(uint32_t now)
{
    if (trigPin == 255 || echoPin == 255)
    {
        return;
    }

    if (now - lastSampleTime < sampleIntervalMs)
    {
        return;
    }
    lastSampleTime = now;

    uint16_t measuredDistance = readDistanceOnce();

    // Descartar ecos inverosímiles por debajo de la zona ciega (< 3 cm)
    bool rawDetect = (measuredDistance >= MIN_VALID_DISTANCE_CM && measuredDistance <= thresholdCm);

    // Preservar la distancia válida detectada durante la presencia del vehículo
    if (rawDetect || !vehiclePresent)
    {
        lastDistanceCm = measuredDistance;
    }

    // Filtro anti-rebote mediante confirmaciones de muestras consecutivas
    if (rawDetect != vehiclePresent)
    {
        debounceCounter++;
        if (debounceCounter >= US_DEBOUNCE_COUNT)
        {
            vehiclePresent = rawDetect;
            debounceCounter = 0;

            if (vehiclePresent)
            {
                arrivedEvent = true;
            }
            else
            {
                clearedEvent = true;
            }
        }
    }
    else
    {
        debounceCounter = 0;
    }
}

bool UltrasonicSensor::isVehiclePresent() const
{
    return vehiclePresent;
}

uint16_t UltrasonicSensor::getDistanceCm() const
{
    return lastDistanceCm;
}

bool UltrasonicSensor::hasVehicleArrived()
{
    if (arrivedEvent)
    {
        arrivedEvent = false;
        return true;
    }
    return false;
}

bool UltrasonicSensor::hasVehicleCleared()
{
    if (clearedEvent)
    {
        clearedEvent = false;
        return true;
    }
    return false;
}
