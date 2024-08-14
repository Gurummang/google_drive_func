package com.GASB.google_drive_func.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MessageMannager {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitTemplate groupingRabbitTemplate;

    private final RabbitTemplate initRabbitTemplate;


    @Autowired
    public MessageMannager(RabbitTemplate rabbitTemplate, RabbitTemplate groupingRabbitTemplate,
                           RabbitTemplate initRabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.groupingRabbitTemplate = groupingRabbitTemplate;
        this.initRabbitTemplate = initRabbitTemplate;
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

    @RabbitListener(queues = "${rabbitmq.init.queue}")
    public void receiveMessage(String message) {
        log.info("Received message from fileQueue: " + message);
        // 여기서 받은 workspace_id를 기반으로 token을 가져와서 사용
        // 아마도 init엔드포인트를 없애고 해당 함수들을 호출하면 되지않을까...
    }
}
