package com.parking.webapp.conf;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // El exchange exclusiva que usa el plugin MQTT de RabbitMQ
    public static final String EXCHANGE_NAME = "amq.topic";

    // Cola exclusiva para el backend
    public static final String BACKEND_QUEUE = "kernel.arduino.queue";

    // Escuchar todos los mensajes que empiecen con kernel.arduino.#
    public static final String ROUTING_KEY_FROM_ARDUINO = "kernel.arduino.#";

    @Bean
    public Queue backendQueue() {
        return new Queue(BACKEND_QUEUE, true);  // True = durable
    }

    @Bean
    public TopicExchange backendExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue backendQueue, TopicExchange mqttExchange) {
        return BindingBuilder.bind(backendQueue)
                .to(mqttExchange)
                .with(ROUTING_KEY_FROM_ARDUINO);
    }

}
