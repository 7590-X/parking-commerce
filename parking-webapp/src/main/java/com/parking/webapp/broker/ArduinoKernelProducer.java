package com.parking.webapp.broker;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import com.parking.webapp.conf.RabbitMQConfig;
import com.parking.webapp.ports.output.HardwareBarrierPort;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArduinoKernelProducer implements HardwareBarrierPort {

    private final RabbitTemplate template;

    private static final String KERNEL_BARRIER_CMD = "kernel.arduino.barrier.cmd";

    /**
     * Envio de comando para forzado de apertura de talanquera
     * 
     * @param cmd
     */
    public void sendBarrierCommand(String cmd) {
        log.info("Enviando comando a la barrera {}", cmd);
        template.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, KERNEL_BARRIER_CMD, cmd);
    }

    /**
     * Envio de response al boker para el kernel arduino
     * 
     * @param payload Carga de respuesta para el kernel
     */
    public void sendMQResponse(String routingKey, Object payload) {
        log.info("Publicando mensaje [{}]: {}", routingKey, payload);
        template.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, routingKey, payload);
    }
}
