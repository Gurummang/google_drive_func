package com.GASB.google_drive_func.service.file;

import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.GASB.google_drive_func.service.DriveApiService;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class DriveFileSerivce {

    public final DriveApiService driveApiService;
    public final FileUtil fileUtil;
    public final OrgSaaSRepo orgSaaSRepo;
    public final GoogleUtil googleUtil;
    public final WorkspaceConfigRepo workspaceConfigRepo;

    private static final List<String> spaceIdList = List.of("0ABuaF1uQELhqUk9PVA", "0ABfguJfTe9jEUk9PVA");
    @Autowired
    public DriveFileSerivce(DriveApiService driveApiService, FileUtil fileUtil, OrgSaaSRepo orgSaaSRepo, GoogleUtil googleUtil, WorkspaceConfigRepo worekSpaceRepo) {
        this.driveApiService = driveApiService;
        this.fileUtil = fileUtil;
        this.orgSaaSRepo = orgSaaSRepo;
        this.googleUtil = googleUtil;
        this.workspaceConfigRepo = worekSpaceRepo;
    }

    @Transactional
    public CompletableFuture<Void> fetchAndStoreFiles(int workspace_id, String event_type){
        try {
            Drive service = googleUtil.getDriveService(workspace_id);
            OrgSaaS orgSaaSObject = orgSaaSRepo.findById(workspace_id).orElse(null);

            List<CompletableFuture<Void>> futures = new ArrayList<>();

            for (String spaceId : spaceIdList) {
                List<File> fileList = driveApiService.fetchFiles(service, spaceId).getFiles();
                for (File file : fileList) {
                    if (shouldSkipFile(file)) {
                        log.info("File is only supported for Google Drive, skipping: {}, {}", file.getId(), file.getName());
                        continue;
                    }
                    CompletableFuture<Void> future = fileUtil.processAndStoreFile(file, orgSaaSObject, workspace_id, event_type, service);
                    futures.add(future);
                }
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        } catch (Exception e) {
            log.error("Error processing files", e);
            return CompletableFuture.completedFuture(null);
        }
    }


    private boolean shouldSkipFile(File file) {
        return "application/vnd.google-apps.spreadsheet".equalsIgnoreCase(file.getMimeType()) ||  // Google Sheets
                "application/vnd.google-apps.document".equalsIgnoreCase(file.getMimeType()) ||    // Google Docs
                "application/vnd.google-apps.presentation".equalsIgnoreCase(file.getMimeType());  // Google Slides
    }





}