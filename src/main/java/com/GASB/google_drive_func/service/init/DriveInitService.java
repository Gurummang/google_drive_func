package com.GASB.google_drive_func.service.init;

import com.GASB.google_drive_func.service.DriveUserService;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleDriveWebhookManager;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.GASB.google_drive_func.service.file.DriveFileService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class DriveInitService {

    private final DriveFileService driveFileService;
    private final DriveUserService driveUserService;
    private final GoogleDriveWebhookManager googleDriveWebhookManager;

    @Autowired
    public DriveInitService(DriveFileService driveFileService, DriveUserService driveUserService, GoogleDriveWebhookManager googleDriveWebhookManager) {
        this.driveFileService = driveFileService;
        this.driveUserService = driveUserService;
        this.googleDriveWebhookManager = googleDriveWebhookManager;
    }

    @Async
    public CompletableFuture<Void> fetchAndSaveFiles(int workspaceId) {
        return driveFileService.fetchAndStoreFiles(workspaceId, "file_upload")
                .thenRun(() -> log.info("Files saved successfully"))
                .exceptionally(e -> {
                    log.error("Error fetching files: {}", e.getMessage(), e);
                    return null;
                });
    }

    @Async
    public CompletableFuture<Void> fetchAndSaveUsers(int workspaceId) {
        return driveUserService.fetchUser(workspaceId)
                .thenRun(() -> log.info("Users fetched successfully"))
                .exceptionally(e -> {
                    log.error("Error fetching users: {}", e.getMessage(), e);
                    return null;
                });
    }

    @Transactional
    public void fetchAndSaveAll(int workspaceId) {
        CompletableFuture<Void> usersFuture = fetchAndSaveUsers(workspaceId);
        CompletableFuture<Void> filesFuture = fetchAndSaveFiles(workspaceId);

        // filesFuture이 끝난 이후 웹훅 설정 하도록 함
        try {
            filesFuture
                    .thenCompose(v -> googleDriveWebhookManager.setWebhook(workspaceId))
                    .thenRun(() -> log.info("Webhook set successfully"))
                    .thenCombine(usersFuture, (f,u)-> {
                        log.info("All data fetched and saved successfully");
                        return null;
                    })
                    .exceptionally(e -> {
                        log.error("Error fetching files, users, or setting webhook: {}", e.getMessage(), e);
                        return null;
                    })
                    .join();
        } catch (Exception e){
            log.error("Error fetching files, users, or setting webhook: {}", e.getMessage(),e);
        }
    }
}
