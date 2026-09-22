package com.parking.webapp.ports.output;

/**
 * Puerto de salida para el control de barreras físicas y comunicación hacia dispositivos microcontroladores.
 * Aplica el principio de Inversión de Dependencias (DIP) para que el servicio de parqueo
 * no dependa de implementaciones concretas de RabbitMQ o Arduino.
 */
public interface HardwareBarrierPort {

    /**
     * Envía la orden física de apertura de la talanquera de salida.
     *
     * @param command Comando específico (e.g. "OPEN").
     */
    void sendBarrierCommand(String command);

    /**
     * Publica una respuesta formal de control de acceso hacia el canal del microcontrolador.
     *
     * @param routingKey Clave de enrutamiento de respuesta.
     * @param payload    Carga de respuesta (e.g. ALLOW / DENY).
     */
    void sendMQResponse(String routingKey, Object payload);
}
