package com.GASB.google_drive_func.service.event;

import com.GASB.google_drive_func.service.file.DriveFileService;
import com.GASB.google_drive_func.service.init.DriveInitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MessageReceiver {

    private final DriveInitService driveInitService;
    private final DriveFileService driveFileService;

    @Autowired
    public MessageReceiver(DriveInitService driveInitService, DriveFileService driveFileService) {
        this.driveInitService = driveInitService;
        this.driveFileService = driveFileService;
    }
    @RabbitListener(queues = "${rabbitmq.init.queue}")
    public void receiveMessage(int message) {
        try {
            log.info("Received message from queue: " + message);
            driveInitService.fetchAndSaveAll(message);
        } catch (RuntimeException e) {
            log.error("Error receiving message: {}", e.getMessage());
        }
    }


    @RabbitListener(queues = "${rabbitmq.GOOGLE_DELETE_QUEUE}")
    public void receiveDeleteMessage(List<Map<String,String>> message){
        try {
            log.info("Received message from queue: " + message);
            driveFileService.fileDelete(message);
        } catch (RuntimeException e) {
            log.error("Error receiving message: {}", e.getMessage());
        }
    }

}
