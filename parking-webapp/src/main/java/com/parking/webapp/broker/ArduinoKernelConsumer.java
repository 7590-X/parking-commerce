package com.parking.webapp.broker;

import com.parking.webapp.conf.RabbitMQConfig;
import com.parking.webapp.service.ParkingBrokerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArduinoKernelConsumer {

    // Routing Keys
    private static final String TOPIC_REQUEST_IN = "kernel.arduino.request.in";
    private static final String TOPIC_REQUEST_OUT = "kernel.arduino.request.out";

    private final ParkingBrokerService parkingService;

    @RabbitListener(queues = RabbitMQConfig.KERNEL_QUEUE)
    public void receiveMessage(Message message) {
        String topic = message.getMessageProperties().getReceivedRoutingKey();
        String payload = new String(message.getBody());

        log.info("[{}] {}", topic, payload);

        if (topic == null) {
            log.warn("Mensaje recibido sin routing key");
            return;
        }

        switch (topic) {
            case TOPIC_REQUEST_IN -> parkingService.verifyParkingSpace(payload);
            case TOPIC_REQUEST_OUT -> parkingService.checkOutQR(payload);
            default -> log.warn("Tópico no reconocido o no manejado: {}", topic);
        }
    }
}