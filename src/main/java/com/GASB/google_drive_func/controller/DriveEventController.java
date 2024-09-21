package com.GASB.google_drive_func.controller;

import com.GASB.google_drive_func.service.DriveApiService;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.GASB.google_drive_func.service.event.GoogleDriveEvent;
import com.google.api.services.drive.Drive;
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
    private final GoogleUtil googleUtil;
    private final DriveApiService driveApiService;

    @Autowired
    public DriveEventController(GoogleDriveEvent googleDriveEvent, GoogleUtil googleUtil, DriveApiService driveApiService) {
        this.googleDriveEvent = googleDriveEvent;
        this.googleUtil = googleUtil;
        this.driveApiService = driveApiService;
    }
    @PostMapping("/file-change")
    public ResponseEntity<String> handleFileChangedEvent(@RequestBody Map<String, Object> payload) throws Exception {
        log.info("File changed event received: {}", payload);

        int workspaceId = Integer.parseInt(payload.get("workspaceId").toString());

        Drive service = googleUtil.getDriveService(workspaceId);
        String resource_id = payload.get("resourceUri").toString();
        Map<String,String> eventType = driveApiService.getFileDetails(service,resource_id);
        String file_id = eventType.get("fileId");


        switch (eventType.get("eventType")) {
            case "create" -> {
                log.info("File create event received");
                googleDriveEvent.handledCreateEvent(payload,file_id);
            }
            case "update" -> {
                log.info("File update event received");
                googleDriveEvent.handledUpdateEvent(payload,file_id);
            }
            case "delete" -> {
                log.info("File delete event received");
                googleDriveEvent.handledDeleteEvent(payload,file_id);
            }
            default -> log.info("Unknown event received");
        }
        return ResponseEntity.ok("File Change Event received and logged");
    }
}
