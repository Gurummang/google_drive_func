package com.GASB.google_drive_func.service.event;

import com.GASB.google_drive_func.model.entity.Activities;
import com.GASB.google_drive_func.model.entity.MonitoredUsers;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.repository.files.FileActivityRepo;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
import com.GASB.google_drive_func.service.DriveApiService;
import com.GASB.google_drive_func.service.GoogleDriveEventObject;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.GASB.google_drive_func.service.file.FileUtil;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Service
@Slf4j
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

    @Autowired
    public GoogleDriveEvent(WorkspaceConfigRepo workspaceConfigRepo, OrgSaaSRepo orgSaaSRepo, DriveApiService driveApiService,
                            MonitoredUserRepo monitoredUserRepo, FileUtil fileUtil, GoogleUtil googleUtil, FileActivityRepo fileActivityRepo) {
        this.workspaceConfigRepo = workspaceConfigRepo;
        this.orgSaaSRepo = orgSaaSRepo;
        this.fileUtil = fileUtil;
        this.driveApiService = driveApiService;
        this.monitoredUserRepo = monitoredUserRepo;
        this.googleutil = googleUtil;
        this.fileActivityRepo = fileActivityRepo;
    }


    public void handledCreateEvent(Map<String, Object> payload) throws Exception {
        // GoogleDriveEventObject 객체를 payload로 생성
        GoogleDriveEventObject googleDriveEventObject = GoogleDriveEventObject.fromPayload(
                payload, workspaceConfigRepo, orgSaaSRepo, driveApiService, monitoredUserRepo, googleutil
        );

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
        GoogleDriveEventObject googleDriveEventObject = GoogleDriveEventObject.fromPayload(
                payload, workspaceConfigRepo, orgSaaSRepo, driveApiService, monitoredUserRepo, googleutil
        );
        fileUtil.processAndStoreFile(
                googleDriveEventObject.getFile(),
                googleDriveEventObject.getOrgSaaS(),
                googleDriveEventObject.getWorkspaceId(),
                "file_change",
                googleDriveEventObject.getDriveService()
        );
    }

    public void handledDeleteEvent(Map<String, Object> payload) throws Exception {
        GoogleDriveEventObject googleDriveEventObject = GoogleDriveEventObject.fromPayload(
                payload, workspaceConfigRepo, orgSaaSRepo, driveApiService, monitoredUserRepo, googleutil
        );
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
