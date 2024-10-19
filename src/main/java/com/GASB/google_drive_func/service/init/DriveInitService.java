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
        try {
            // 1. 유저 정보 비동기 처리
            CompletableFuture<Void> usersFuture = fetchAndSaveUsers(workspaceId)
                    // 2. 유저 정보 저장이 끝나면 파일 정보 비동기 처리
                    .thenCompose(v -> fetchAndSaveFiles(workspaceId))
                    // 3. 파일 저장이 끝나면 웹훅 설정 비동기 처리
                    .thenCompose(v -> googleDriveWebhookManager.setWebhook(workspaceId))
                    // 4. 모든 작업 완료 후 로그 출력
                    .thenRun(() -> log.info("All data fetched, files saved, and webhook set successfully"))
                    // 예외 처리
                    .exceptionally(e -> {
                        log.error("Error fetching users, files, or setting webhook: {}", e.getMessage(), e);
                        return null;
                    });

            // 모든 비동기 작업 완료 대기
            usersFuture.join();
        } catch (Exception e) {
            log.error("Error in fetchAndSaveAll: {}", e.getMessage(), e);
        }
    }

}
