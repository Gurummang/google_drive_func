package com.GASB.google_drive_func.service;

import com.GASB.google_drive_func.model.entity.MonitoredUsers;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.mapper.DriveFileMapper;
import com.GASB.google_drive_func.model.mapper.DriveUserMapper;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
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

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DriveApiService {

    private final GoogleUtil googleUtil;
    private final FileUtil fileUtil;
    private final OrgSaaSRepo OrgSaaSRepo;
    private final MonitoredUserRepo monitoredUserRepo;
    private final DriveUserMapper DriveUserMapper;
    private final DriveFileMapper DriveFileMapper;
    @Autowired
    public DriveApiService(GoogleUtil googleUtil, FileUtil fileUtil, OrgSaaSRepo OrgSaaSRepo, MonitoredUserRepo MonitoredUsersRepo, DriveUserMapper DriveUserMapper, DriveFileMapper driveFileMapper) {
        this.googleUtil = googleUtil;
        this.fileUtil = fileUtil;
        this.OrgSaaSRepo = OrgSaaSRepo;
        this.DriveFileMapper = driveFileMapper;
        this.monitoredUserRepo = MonitoredUsersRepo;
        this.DriveUserMapper = DriveUserMapper;
    }


    public FileList fetchFiles(Drive service, String DriveId){
        try {
            FileList result = service.files().list()
                    .setDriveId(DriveId)
                    .setIncludeItemsFromAllDrives(true)
                    .setSupportsAllDrives(true)
                    .setCorpora("drive")
                    .setQ("trashed = false")
                    .setFields("files")
                    .execute();
            return result;
        } catch (Exception e) {
            log.error("An error occurred while listing files: {}", e.getMessage(), e);
            return null;
        }
    }

    // 공유 드라이브에서 파일을 다운로드하고 저장
//    @Async("threadPoolTaskExecutor")
//    public CompletableFuture<Void> fetchFileAsync(int workspace_id, String sharedDriveId) {
//        return CompletableFuture.runAsync(() -> {
//            try {
//                Drive service = googleUtil.getDriveService(workspace_id);
//                OrgSaaS orgSaaSObject = OrgSaaSRepo.findById(workspace_id).orElse(null);
//                Path basePath = Paths.get("downloaded_files");
//                FileList result = service.files().list()
//                        .setDriveId(sharedDriveId)
//                        .setIncludeItemsFromAllDrives(true)
//                        .setSupportsAllDrives(true)
//                        .setCorpora("drive")
//                        .setQ("trashed = false")
//                        .setFields("files(id, name, mimeType, parents, size, owners(displayName, emailAddress))")
//                        .execute();
//
//                log.info("Files found in shared drive {}: {}", sharedDriveId, result.getFiles().size());
//                if (result.getFiles() != null && !result.getFiles().isEmpty()) {
//                    result.getFiles().forEach(file -> {
//                        log.info("File ID: {}, Name: {}", file.getId(), file.getName());
//                        String savedPath = fileUtil.getFullPath(file, service);
//                        fileUtil.DownloadFileMethod(file.getId(), basePath.resolve(file.getName()).toString(), service);
//                        MonitoredUsers user = MonitoredUsersRepo.findByEmail(file.getOwners().get(0).getEmailAddress()).orElse(null);
//                        DriveFileMapper.EntireEntityConverter(file, null, user, savedPath, fileUtil.calculateHash(file), savedPath, orgSaaSObject);
//                    });
//                } else {
//                    log.info("No files found in the shared drive.");
//                }
//            } catch (Exception e) {
//                log.error("An error occurred while listing files: {}", e.getMessage(), e);
//            }
//        });
//    }


    // 공유 드라이브에서 사용자의 권한 리스트를 가져옴
    public void fetchUserList(int workspace_id, String sharedDriveId) {
        try {
            Drive service = googleUtil.getDriveService(workspace_id);
            OrgSaaS orgSaaSObject = OrgSaaSRepo.findById(workspace_id).orElse(null);

            // 먼저 공유 드라이브 정보를 가져옵니다.
            com.google.api.services.drive.model.Drive sharedDrive = service.drives().get(sharedDriveId).execute();

            // 그 다음 권한 목록을 가져옵니다.
            PermissionList permissions = service.permissions().list(sharedDriveId)
                    .setSupportsAllDrives(true)
                    .setUseDomainAdminAccess(true)
                    .setFields("permissions(id,emailAddress,displayName,role)")
                    .execute();

            List<String> members = permissions.getPermissions().stream()
                    .map(Permission::getEmailAddress)
                    .filter(Objects::nonNull)
                    .toList();

            System.out.println("members: " + members);

            if (!permissions.getPermissions().isEmpty()) {
                for (Permission permission : permissions.getPermissions()) {
                    if (permission.getEmailAddress() != null) {
                        log.info("Permission ID: {}, Role: {}, Email: {}, DisplayName: {}",
                                permission.getId(), permission.getRole(), permission.getEmailAddress(), permission.getDisplayName());
                        MonitoredUsers user = DriveUserMapper.toEntity(permission, orgSaaSObject);
                        if (!monitoredUserRepo.existsByUserId(permission.getId())) {
                            try{
                                monitoredUserRepo.save(user);
                            } catch (Exception e) {
                                log.error("An error occurred while saving user: {}", e.getMessage(), e);
                            }
                            log.info("Permission ID: {}, Role: {}, Email: {}",
                                    permission.getId(), permission.getRole(), permission.getEmailAddress());
                        }
                    }
                }
            } else {
                log.info("No permissions found for the shared drive with ID: {}", sharedDriveId);
            }
        } catch (Exception e) {
            log.error("An error occurred while listing permissions for the shared drive: {}", e.getMessage(), e);
        }
    }
}
