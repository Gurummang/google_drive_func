package com.GASB.google_drive_func.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageSender {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTemplate groupingRabbitTemplate;

    @Autowired
    public MessageSender(RabbitTemplate rabbitTemplate, RabbitTemplate groupingRabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.groupingRabbitTemplate = groupingRabbitTemplate;
    }

    public void sendMessage(Long message) {
        rabbitTemplate.convertAndSend(message);
        log.info("Sent message to default queue: " + message);
    }

    public void sendGroupingMessage(Long message) {
        groupingRabbitTemplate.convertAndSend(message);
        log.info("Sent message to grouping queue: " + message);
    }

    public void sendMessageToQueue(Long message, String queueName) {
        rabbitTemplate.convertAndSend(queueName, message);
        log.info("Sent message to queue " + queueName + ": " + message);
    }
}
