#include "UltrasonicSensor.h"

UltrasonicSensor::UltrasonicSensor()
    : trigPin(255),
      echoPin(255),
      thresholdCm(US_DETECT_THRESHOLD_CM),
      sampleIntervalMs(US_SAMPLE_INTERVAL_MS),
      timeoutUs(US_TIMEOUT_US),
      lastSampleTime(0),
      lastDistanceCm(999),
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
    lastDistanceCm = 999;
    vehiclePresent = false;
    debounceCounter = 0;
    arrivedEvent = false;
    clearedEvent = false;
}

uint16_t UltrasonicSensor::readDistanceOnce()
{
    if (trigPin == 255 || echoPin == 255) return 999;

    // Pulso de disparo de 10 microsegundos
    digitalWrite(trigPin, LOW);
    delayMicroseconds(2);
    digitalWrite(trigPin, HIGH);
    delayMicroseconds(10);
    digitalWrite(trigPin, LOW);

    // Medición con timeout acotado (máximo ~5.8 ms para 1 metro)
    // Evita totalmente el bloqueo por defecto de 1 segundo de pulseIn
    unsigned long duration = pulseIn(echoPin, HIGH, timeoutUs);

    if (duration == 0)
    {
        // Sin eco recibido dentro del rango acotado
        return 999;
    }

    // Cálculo de distancia en cm (velocidad sonido ~343 m/s = 29.1 us/cm ida o 58.2 us ida y vuelta)
    return (uint16_t)(duration / 58UL);
}

void UltrasonicSensor::update(uint32_t now)
{
    if (trigPin == 255 || echoPin == 255) return;

    if (now - lastSampleTime < sampleIntervalMs)
    {
        return;
    }
    lastSampleTime = now;

    uint16_t measuredDistance = readDistanceOnce();
    lastDistanceCm = measuredDistance;

    bool rawDetect = (measuredDistance > 0 && measuredDistance <= thresholdCm);

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
