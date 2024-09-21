package com.GASB.google_drive_func.service;

import com.GASB.google_drive_func.model.entity.MonitoredUsers;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.GASB.google_drive_func.service.DriveApiService;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.util.Map;

public class GoogleDriveEventObject {

    private final int workspaceId;
    private final OrgSaaS orgSaasObj;
    private final Drive driveService;
    private final File file;
    private final MonitoredUsers monitoredUser;

    // 기존 레포지토리 및 서비스들
    private final WorkspaceConfigRepo workspaceConfigRepo;
    private final OrgSaaSRepo orgSaaSRepo;
    private final DriveApiService driveApiService;
    private final MonitoredUserRepo monitoredUserRepo;
    private final GoogleUtil googleUtil;

    // 정적 팩토리 메서드로 payload를 처리하는 생성 방식 추가
    public static GoogleDriveEventObject fromPayload(Map<String, Object> payload, WorkspaceConfigRepo workspaceConfigRepo,
                                                     OrgSaaSRepo orgSaaSRepo, DriveApiService driveApiService,
                                                     MonitoredUserRepo monitoredUserRepo, GoogleUtil googleUtil, String file_id) throws Exception {
        // workspaceId 추출 및 OrgSaaS 조회
        int workspaceId = workspaceConfigRepo.getWorkspaceConfigId(payload.get("workspaceId").toString()).orElse(null);
        OrgSaaS orgSaasObj = orgSaaSRepo.findById(workspaceId).orElse(null);

        // Drive API 서비스 객체 생성
        Drive service = googleUtil.getDriveService(workspaceId);

        // 파일 및 마지막 수정자 정보 조회
        File file = driveApiService.fetchOneFile(file_id, workspaceId, service);
        String lastModifiedUser = file.getLastModifyingUser().getEmailAddress();
        MonitoredUsers monitoredUser = monitoredUserRepo.findByEmail(lastModifiedUser).orElse(null);

        // 객체 생성 및 반환
        return new GoogleDriveEventObject(workspaceId, orgSaasObj, service, file, monitoredUser,
                workspaceConfigRepo, orgSaaSRepo, driveApiService, monitoredUserRepo, googleUtil);
    }

    // 기존 생성자
    private GoogleDriveEventObject(int workspaceId, OrgSaaS orgSaasObj, Drive driveService, File file,
                                   MonitoredUsers monitoredUser, WorkspaceConfigRepo workspaceConfigRepo,
                                   OrgSaaSRepo orgSaaSRepo, DriveApiService driveApiService,
                                   MonitoredUserRepo monitoredUserRepo, GoogleUtil googleUtil) {
        this.workspaceId = workspaceId;
        this.orgSaasObj = orgSaasObj;
        this.driveService = driveService;
        this.file = file;
        this.monitoredUser = monitoredUser;
        this.workspaceConfigRepo = workspaceConfigRepo;
        this.orgSaaSRepo = orgSaaSRepo;
        this.driveApiService = driveApiService;
        this.monitoredUserRepo = monitoredUserRepo;
        this.googleUtil = googleUtil;
    }

    // 필요한 getter 메서드 추가
    public int getWorkspaceId() {
        return workspaceId;
    }

    public OrgSaaS getOrgSaaS() {
        return orgSaasObj;
    }

    public Drive getDriveService() {
        return driveService;
    }

    public File getFile() {
        return file;
    }

    public MonitoredUsers getMonitoredUser() {
        return monitoredUser;
    }
}
