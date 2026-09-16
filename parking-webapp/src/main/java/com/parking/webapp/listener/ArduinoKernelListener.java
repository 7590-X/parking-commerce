package com.parking.webapp.listener;

import com.parking.webapp.conf.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArduinoKernelListener {

    private final RabbitTemplate rabbitTemplate;
    private static final String ROUTING_KEY_FROM_BACKEND = "backend.arduino.commands";


    @RabbitListener(queues = RabbitMQConfig.BACKEND_QUEUE)
    public void receiveMessage(Message message) {
        String topic = message.getMessageProperties().getReceivedRoutingKey();
        String payload = new String(message.getBody());
        log.info("[{}] {}", topic, payload);
    }

    public void sendMessage(String command) {
        rabbitTemplate.convertAndSend(ROUTING_KEY_FROM_BACKEND, command);
        log.info("Message sent: {}", command);
    }
}
