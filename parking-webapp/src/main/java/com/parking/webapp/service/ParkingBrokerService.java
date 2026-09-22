package com.parking.webapp.service;

/**
 * Interfaz para la atención y procesamiento de eventos provenientes del broker de mensajería (RabbitMQ/MQTT)
 * asociados a las acciones del microcontrolador/Arduino del parqueo.
 */
public interface ParkingBrokerService {

    /**
     * Verifica la disponibilidad de espacio en el estacionamiento a partir del payload recibido
     * desde el microcontrolador al momento de solicitar una entrada. Si hay espacio, genera un ticket
     * y envía la señal de acceso permitido (ALLOW); de lo contrario, envía denegado (DENY).
     *
     * @param payload Comando recibido desde el broker (se espera {@code CHECK_SPACE}).
     */
    void verifyParkingSpace(String payload);

    /**
     * Procesa la solicitud de verificación de ticket QR recibida desde el broker al momento de la salida.
     *
     * @param payload Comando recibido desde el broker (se espera {@code CHECK_QR}).
     */
    void checkOutQR(String payload);
}

