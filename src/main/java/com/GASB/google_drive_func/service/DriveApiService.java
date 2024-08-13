package com.GASB.google_drive_func.service;

import com.GASB.google_drive_func.model.entity.ChannelList;
import com.GASB.google_drive_func.model.entity.MonitoredUsers;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.mapper.DriveChannelMapper;
import com.GASB.google_drive_func.model.mapper.DriveUserMapper;
import com.GASB.google_drive_func.model.repository.MonitoredUserRepo;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.GASB.google_drive_func.service.file.FileUtil;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
@Service
@Slf4j
public class DriveApiService {

    private final GoogleUtil googleUtil;
    private final FileUtil fileUtil;
    private final OrgSaaSRepo OrgSaaSRepo;
    private final MonitoredUserRepo MonitoredUsersRepo;
    private final DriveUserMapper DriveUserMapper;
    @Autowired
    public DriveApiService(GoogleUtil googleUtil, FileUtil fileUtil, OrgSaaSRepo OrgSaaSRepo, MonitoredUserRepo MonitoredUsersRepo, DriveUserMapper DriveUserMapper) {
        this.googleUtil = googleUtil;
        this.fileUtil = fileUtil;
        this.OrgSaaSRepo = OrgSaaSRepo;
        this.MonitoredUsersRepo = MonitoredUsersRepo;
        this.DriveUserMapper = DriveUserMapper;
    }

    // 공유 드라이브에서 파일을 다운로드하고 저장
    public void fetchAndStoreFiles(int workspace_id, String sharedDriveId) {
        try {
            Drive service = googleUtil.getDriveService(workspace_id);
            Path basePath = Paths.get("downloaded_files");
            FileList result = service.files().list()
                    .setQ("'" + sharedDriveId + "' in parents")
                    .setFields("files(id, name)")
                    .execute();
            if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                result.getFiles().forEach(file -> {
                    fileUtil.DownloadFileMethod(file.getId(), basePath.resolve(file.getName()).toString(), service);
                });
            } else {
                log.info("No files found in the shared drive.");
            }
        } catch (Exception e) {
            log.error("An error occurred while listing files: {}", e.getMessage(), e);
        }
    }

    // 공유 드라이브에서 사용자의 권한 리스트를 가져옴
    public void fetchUserList(int workspace_id, String sharedDriveId) {
        try {
            Drive service = googleUtil.getDriveService(workspace_id);
            OrgSaaS orgSaaSObejct = OrgSaaSRepo.findById(workspace_id).orElse(null);
            PermissionList permissions = service.permissions().list(sharedDriveId).execute();
            if (permissions != null && !permissions.getPermissions().isEmpty()) {
                for (Permission permission : permissions.getPermissions()) {
                    MonitoredUsers user = DriveUserMapper.toEntity(permission, orgSaaSObejct);
                    if (!MonitoredUsersRepo.existsByUserId(permission.getId())) {
                        MonitoredUsersRepo.save(user);
                    }
                }
            } else {
                log.info("No permissions found for the folder with ID: {}", sharedDriveId);
            }
        } catch (Exception e) {
            log.error("An error occurred while listing permissions for the folder: {}", e.getMessage(), e);
        }
    }

    // 공유 드라이브에서 폴더 리스트를 가져옴
//    public void fetchChannelList(int workspace_id, String sharedDriveId) {
//        try {
//            OrgSaaS orgSaaS = OrgSaaSRepo.(workspace_id);
//            Drive service = googleUtil.getDriveService(workspace_id);
//            DriveList result = service.drives().list().execute();
//            if (result.getDrives() != null && !result.getDrives().isEmpty()) {
//                result.getDrives().forEach(drive -> {
//                    ChannelList channel = DriveChannelMapper.toEntity();
//                    log.info("Drive ID: {}, Name: {}", drive.getId(), drive.getName());
//                });
//            } else {
//                log.info("No drives found.");
//            }
//        } catch (Exception e) {
//            log.error("An error occurred while listing channels: {}", e.getMessage(), e);
//        }
//    }
}
