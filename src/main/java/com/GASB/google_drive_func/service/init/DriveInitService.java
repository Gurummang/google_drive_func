package com.GASB.google_drive_func.service.init;

import com.GASB.google_drive_func.service.DriveUserService;
import com.GASB.google_drive_func.service.file.DriveFileService;
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

    @Autowired
    public DriveInitService(DriveFileService driveFileService, DriveUserService driveUserService) {
        this.driveFileService = driveFileService;
        this.driveUserService = driveUserService;
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

    public void fetchAndSaveAll(int workspaceId) {
        CompletableFuture<Void> usersFuture = fetchAndSaveUsers(workspaceId);
        CompletableFuture<Void> filesFuture = fetchAndSaveFiles(workspaceId);

        // 두 비동기 작업이 모두 완료될 때까지 기다림
        CompletableFuture.allOf(usersFuture, filesFuture)
                .thenRun(() -> log.info("All data fetched and saved successfully"))
                .exceptionally(e -> {
                    log.error("Error fetching files or users: {}", e.getMessage(), e);
                    return null;
                })
                .join(); // 비동기 작업을 동기적으로 완료되도록 기다림
    }
}
