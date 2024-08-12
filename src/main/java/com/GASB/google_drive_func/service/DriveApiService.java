package com.GASB.google_drive_func.service;

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

    @Autowired
    public DriveApiService(GoogleUtil googleUtil, FileUtil fileUtil) {
        this.googleUtil = googleUtil;
        this.fileUtil = fileUtil;
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
                PermissionList permissions = service.permissions().list(sharedDriveId).execute();
                if (permissions != null && !permissions.getPermissions().isEmpty()) {
                    for (Permission permission : permissions.getPermissions()) {
                        log.info("Permission ID: {}, Role: {}, Type: {}, Email: {}",
                                permission.getId(),
                                permission.getRole(),
                                permission.getType(),
                                permission.getEmailAddress());
                    }
                } else {
                    log.info("No permissions found for the folder with ID: {}", sharedDriveId);
                }
            } catch (Exception e) {
                log.error("An error occurred while listing permissions for the folder: {}", e.getMessage(), e);
            }
        }

    // 공유 드라이브에서 폴더 리스트를 가져옴
    public void fetchChannelList(int workspace_id, String sharedDriveId) {
        try {
            Drive service = googleUtil.getDriveService(workspace_id);
            FileList result = service.files().list()
                    .setQ("'" + sharedDriveId + "' in parents and mimeType = 'application/vnd.google-apps.folder'")
                    .setFields("files(id, name)")
                    .execute();
            if (result.getFiles() != null && !result.getFiles().isEmpty()) {
                result.getFiles().forEach(folder -> {
                    log.info("Folder found: {} ({})", folder.getName(), folder.getId());
                });
            } else {
                log.info("No folders found in the shared drive.");
            }
        } catch (Exception e) {
            log.error("An error occurred while listing channels: {}", e.getMessage(), e);
        }
    }
}
