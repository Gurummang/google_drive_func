package com.GASB.google_drive_func.service.event;

import com.GASB.google_drive_func.model.entity.Activities;
import com.GASB.google_drive_func.model.entity.MonitoredUsers;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.repository.files.FileActivityRepo;
import com.GASB.google_drive_func.model.repository.files.FileUploadRepository;
import com.GASB.google_drive_func.model.repository.files.StoredFileRepository;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
import com.GASB.google_drive_func.service.DriveApiService;
import com.GASB.google_drive_func.service.GoogleDriveEventObject;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.GASB.google_drive_func.service.file.FileUtil;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleDriveEvent {

    private final int saas_id = 6;

    private final WorkspaceConfigRepo workspaceConfigRepo;
    private final OrgSaaSRepo orgSaaSRepo;
    private final DriveApiService driveApiService;
    private final MonitoredUserRepo monitoredUserRepo;
    private final FileUtil fileUtil;
    private final GoogleUtil googleutil;
    private final FileActivityRepo fileActivityRepo;
    private final StoredFileRepository storedFileRepository;
    private final FileUploadRepository fileUploadRepository;
    private final MessageSender messageSender;

    // 사용자 조회, Org SaaS객체 생성, Drive API 서비스 객체 생성, 파일 정보 조회 등등..
    private GoogleDriveEventObject createGoogleDriveEventObject(int workspace_id, String file_id) throws Exception {
        try {
            return GoogleDriveEventObject.fromPayload(
                    workspace_id, workspaceConfigRepo, orgSaaSRepo, driveApiService, monitoredUserRepo, googleutil, file_id
            );
        } catch (IllegalArgumentException e) {
            log.error("Error creating GoogleDriveEventObject", e);
            throw new IllegalArgumentException("Error creating GoogleDriveEventObject");
        } catch (Exception e) {
            log.error("Error creating GoogleDriveEventObject", e);
            throw new RuntimeException("Error creating GoogleDriveEventObject", e);
        }
    }

    public void handledCreateEvent(int workspace_id, String file_id) throws Exception {
        try {
            GoogleDriveEventObject googleDriveEventObject = createGoogleDriveEventObject(workspace_id,file_id);
            fileUtil.processAndStoreFile(
                    googleDriveEventObject.getFile(),
                    googleDriveEventObject.getOrgSaaS(),
                    googleDriveEventObject.getWorkspaceId(),
                    "file_upload",
                    googleDriveEventObject.getDriveService()
            );
        } catch (IllegalArgumentException e) {
            log.error("Error handling file create event", e);
            throw new IllegalArgumentException("Error handling file create event");
        } catch (NullPointerException e){
            log.error("Error handling file create event", e);
            throw new NullPointerException("Error handling file create event");
        } catch (Exception e) {
            log.error("Error handling file create event", e);
            throw new RuntimeException("Error handling file create event", e);
        }
    }

    public void handledUpdateEvent(int workspace_id, String file_id) throws Exception {
        try {
            GoogleDriveEventObject googleDriveEventObject = createGoogleDriveEventObject(workspace_id,file_id);
            fileUtil.processAndStoreFile(
                    googleDriveEventObject.getFile(),
                    googleDriveEventObject.getOrgSaaS(),
                    googleDriveEventObject.getWorkspaceId(),
                    "file_change",
                    googleDriveEventObject.getDriveService()
            );
        } catch (IllegalArgumentException e) {
            log.error("Error handling file update event", e);
            throw new IllegalArgumentException("Error handling file update event");
        } catch (NullPointerException e){
            log.error("Error handling file update event", e);
            throw new NullPointerException("Error handling file update event");
        } catch (Exception e) {
            log.error("Error handling file update event", e);
            throw new RuntimeException("Error handling file update event", e);
        }
    }

    @Transactional
    public void handledDeleteEvent(int workspace_id, String fileId) throws Exception {
        GoogleDriveEventObject googleDriveEventObject;
        try {
            googleDriveEventObject = createGoogleDriveEventObject(workspace_id,fileId);
        } catch (RuntimeException e){
            log.error("Error handling file delete event", e);
            throw new RuntimeException("Error handling file delete event");
        }

        try {
            long timestamp = Instant.now().getEpochSecond();
            String file_id = googleDriveEventObject.getFile().getId();
            Activities activities = copyForDelete(file_id, timestamp);
            String file_hash = fileUploadRepository.findFileHashByFileId(file_id).orElse(null);
            String s3Path = storedFileRepository.findSavePathByHash(file_hash).orElse(null);

            if (s3Path == null) {
                log.error("Failed to find S3 path for file: {}", file_id);
                throw new IllegalStateException("No S3 path found for file: " + file_id);
            }

            fileActivityRepo.save(activities);
            fileUtil.deleteFileInS3(s3Path);

            // file_upload 테이블에서 deleted 컬럼을 true로 변경
            fileUploadRepository.checkDelete(file_id);

            // 메시지 전송
            messageSender.sendGroupingMessage(activities.getId());
        } catch (IllegalArgumentException e) {
            log.error("Error handling file delete event", e);
            throw new IllegalArgumentException("Error handling file delete event");
        } catch (Exception e) {
            log.error("Error handling file delete event", e);
            throw new RuntimeException("Error handling file delete event", e);
        }
    }

    private Activities copyForDelete(String file_id, long timestamp) {
        try {
            Activities activities = fileActivityRepo.findRecentBySaasFileId(file_id).orElse(null);

            if (activities == null) {
                log.warn("No recent activities found for file_id: {}", file_id);
                throw new IllegalStateException("No recent activity found for file: " + file_id);
            }

            ZoneId zoneId = ZoneId.of("Asia/Seoul");
            LocalDateTime adjustedTimestamp;

            if (timestamp > 0) {
                adjustedTimestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), zoneId);
            } else {
                adjustedTimestamp = LocalDateTime.now(zoneId);
            }

            return Activities.builder()
                    .user(activities.getUser())
                    .eventType("file_delete")
                    .saasFileId(file_id)
                    .fileName(activities.getFileName())
                    .eventTs(adjustedTimestamp)
                    .uploadChannel(activities.getUploadChannel())
                    .tlsh(activities.getTlsh())
                    .build();

        } catch (IllegalArgumentException e){
            log.error("Error copying activity for delete: {}", e.getMessage());
            throw new IllegalArgumentException("Error copying activity for delete");
        } catch (Exception e) {
            log.error("Error copying activity for delete: {}", e.getMessage());
            throw new RuntimeException("Error copying activity for delete", e);
        }
    }
}
