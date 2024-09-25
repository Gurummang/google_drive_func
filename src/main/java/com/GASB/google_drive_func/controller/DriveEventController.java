package com.GASB.google_drive_func.controller;

import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.GASB.google_drive_func.service.DriveApiService;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.GASB.google_drive_func.service.event.GoogleDriveEvent;
import com.google.api.services.drive.Drive;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/api/v1/events/google-drive")
public class DriveEventController {

    private final GoogleDriveEvent googleDriveEvent;
    private final GoogleUtil googleUtil;
    private final DriveApiService driveApiService;
    private final WorkspaceConfigRepo workspaceConfigRepo;

    private final String defaultUrl = "https://back.grummang.com/webhook/GoogleDrive/";
    @Autowired
    public DriveEventController(GoogleDriveEvent googleDriveEvent, GoogleUtil googleUtil,
                                DriveApiService driveApiService, WorkspaceConfigRepo workspaceConfigRepo) {
        this.googleDriveEvent = googleDriveEvent;
        this.googleUtil = googleUtil;
        this.driveApiService = driveApiService;
        this.workspaceConfigRepo = workspaceConfigRepo;
    }
    @PostMapping("/file-change")
    public ResponseEntity<String> handleFileChangedEvent(@RequestBody Map<String, Object> payload) throws Exception {
        log.info("File changed event received: {}", payload);

        String workspace_Id = payload.get("workspaceId").toString();
        String webhookUrl = defaultUrl + workspace_Id;
        int workspaceId = workspaceConfigRepo.getWorkspaceConfigId(webhookUrl);
        Drive service = googleUtil.getDriveService(workspaceId);
        String channel_id = payload.get("channelId").toString();

        List<Map<String,String>> event_detail = driveApiService.getFileDetails(service,channel_id);
        log.info("Event Detail: {}", event_detail);
        for (Map<String,String> detail : event_detail){
            String file_id = detail.get("fileId");
            String event_type = detail.get("eventType");
            log.info("File ID: {}", file_id);
            log.info("Event Type: {}", event_type);

            switch (event_type) {
                case "create" -> {
                    log.info("{} File create event received",file_id);
                    googleDriveEvent.handledCreateEvent(workspaceId,file_id);
                }
                case "update" -> {
                    log.info("{} File update event received",file_id);
                    googleDriveEvent.handledUpdateEvent(workspaceId,file_id);
                }
                case "delete" -> {
                    log.info("{} File delete event received",file_id);
                    googleDriveEvent.handledDeleteEvent(workspaceId,file_id);
                }
                default -> log.info("{} Unknown event received",file_id);
            }
        }
        return ResponseEntity.ok("File Change Event received and logged");
    }
}
