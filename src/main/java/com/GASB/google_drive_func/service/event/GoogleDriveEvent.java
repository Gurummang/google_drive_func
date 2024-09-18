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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class GoogleDriveEvent {

//    public class GoogleDriveChangeEventDto {
//        private String workspaceId;
//        private String channelId;
//        private String resourceId;
//        private String resourceState;
//        private String resourceUri;
//    }

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


    private GoogleDriveEventObject createGoogleDriveEventObject(Map<String, Object> payload) throws Exception {
        return GoogleDriveEventObject.fromPayload(
                payload, workspaceConfigRepo, orgSaaSRepo, driveApiService, monitoredUserRepo, googleutil
        );
    }

    public void handledCreateEvent(Map<String, Object> payload) throws Exception {
        // GoogleDriveEventObject 객체를 payload로 생성
        GoogleDriveEventObject googleDriveEventObject = createGoogleDriveEventObject(payload);

        // 파일 처리 및 저장
        fileUtil.processAndStoreFile(
                googleDriveEventObject.getFile(),
                googleDriveEventObject.getOrgSaaS(),
                googleDriveEventObject.getWorkspaceId(),
                "file_upload",
                googleDriveEventObject.getDriveService()
        );
    }


    public void handledUpdateEvent(Map<String, Object> payload) throws Exception {
        GoogleDriveEventObject googleDriveEventObject = createGoogleDriveEventObject(payload);
        fileUtil.processAndStoreFile(
                googleDriveEventObject.getFile(),
                googleDriveEventObject.getOrgSaaS(),
                googleDriveEventObject.getWorkspaceId(),
                "file_change",
                googleDriveEventObject.getDriveService()
        );
    }

    @Transactional
    public void handledDeleteEvent(Map<String, Object> payload) throws Exception {
        GoogleDriveEventObject googleDriveEventObject = createGoogleDriveEventObject(payload);

        long timestamp = Instant.now().getEpochSecond();
        String file_id = googleDriveEventObject.getFile().getId();
        Activities activities = copyForDelete(file_id, timestamp);
        String file_hash = fileUploadRepository.findFileHashByFileId(file_id).orElse(null);
        String s3Path = storedFileRepository.findSavePathByHash(file_hash).orElse(null);
        fileActivityRepo.save(activities);

        fileUtil.deleteFileInS3(s3Path);
        // 2. file_upload 테이블에서 deleted 컬럼을 true로 변경
        fileUploadRepository.checkDelete(file_id);
        messageSender.sendGroupingMessage(activities.getId());

    }


    private Activities copyForDelete(String file_id, long timestamp){
        // 최근 활동 정보를 찾음, 없으면 null
        Activities activities = fileActivityRepo.findRecentBySaasFileId(file_id).orElse(null);

        // activities가 null일 경우 예외 처리 또는 기본값 처리
        if (activities == null) {
            log.warn("No recent activities found for file_id: {}", file_id);
            throw new IllegalStateException("No recent activity found for file: " + file_id);
        }

        // 시간대를 서울로 고정하여 처리
        ZoneId zoneId = ZoneId.of("Asia/Seoul");

        // timestamp가 0일 경우, 현재 시간을 사용할 수 있도록 처리
        LocalDateTime adjustedTimestamp;
        if (timestamp > 0) {
            adjustedTimestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), zoneId);
        } else {
            adjustedTimestamp = LocalDateTime.now(zoneId);
        }

        return Activities.builder()
                .user(activities.getUser()) // null이 아닌지 확인 후 처리
                .eventType("file_delete")
                .saasFileId(file_id)
                .fileName(activities.getFileName())
                .eventTs(adjustedTimestamp)
                .uploadChannel(activities.getUploadChannel())
                .tlsh(activities.getTlsh())
                .build();
    }
}
