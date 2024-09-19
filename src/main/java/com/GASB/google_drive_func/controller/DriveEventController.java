package com.GASB.google_drive_func.controller;

import com.GASB.google_drive_func.service.event.GoogleDriveEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/v1/events/google-drive")
public class DriveEventController {

    private final GoogleDriveEvent googleDriveEvent;

    @Autowired
    public DriveEventController(GoogleDriveEvent googleDriveEvent) {
        this.googleDriveEvent = googleDriveEvent;
    }
    @PostMapping("/file-change")
    public ResponseEntity<String> handleFileChangedEvent(@RequestBody Map<String, Object> payload) throws Exception {
        log.info("File changed event received: {}", payload);
        String eventType = payload.get("x-goog-resource-state").toString();
        switch (eventType) {
            case "add" -> {
                log.info("File create event received");
                googleDriveEvent.handledCreateEvent(payload);
            }
            case "update" -> {
                log.info("File update event received");
                googleDriveEvent.handledUpdateEvent(payload);
            }
            case "remove" -> {
                log.info("File delete event received");
                googleDriveEvent.handledDeleteEvent(payload);
            }
            default -> log.info("Unknown event received");
        }
        return ResponseEntity.ok("File Change Event received and logged");
    }
}
