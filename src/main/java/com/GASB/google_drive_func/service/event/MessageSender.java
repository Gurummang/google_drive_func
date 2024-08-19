package com.GASB.google_drive_func.service.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageSender {

    private final RabbitTemplate fileQueueRabbitTemplate;
    private final RabbitTemplate groupingRabbitTemplate;


    @Autowired
    public MessageSender(RabbitTemplate rabbitTemplate, RabbitTemplate groupingRabbitTemplate) {
        this.fileQueueRabbitTemplate = rabbitTemplate;
        this.groupingRabbitTemplate = groupingRabbitTemplate;
    }

    public void sendMessage(Long message) {
        fileQueueRabbitTemplate.convertAndSend(message);
        log.info("Sent message to file queue: " + message);
    }

    public void sendGroupingMessage(Long message) {
        groupingRabbitTemplate.convertAndSend(message);
        log.info("Sent message to grouping queue: " + message);
    }

    public void sendMessageToQueue(Long message, String queueName) {
        fileQueueRabbitTemplate.convertAndSend(queueName, message);
        log.info("Sent message to queue " + queueName + ": " + message);
    }
}
