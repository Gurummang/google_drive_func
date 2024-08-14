package com.GASB.google_drive_func.service;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class MessageReceiver {

    @RabbitListener(queues = "${rabbitmq.queue}")
    public void receiveMessage(String message) {
        System.out.println("Received message from fileQueue: " + message);
        // 메시지 처리 로직 추가
    }

    @RabbitListener(queues = "${rabbitmq.GROUPING_QUEUE}")
    public void receiveGroupingMessage(String message) {
        System.out.println("Received message from groupingQueue: " + message);
        // 메시지 처리 로직 추가
    }
}
