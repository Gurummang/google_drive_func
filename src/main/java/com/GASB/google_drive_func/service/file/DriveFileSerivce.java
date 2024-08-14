package com.GASB.google_drive_func.service.file;

import com.GASB.google_drive_func.model.dto.TopUserDTO;
import com.GASB.google_drive_func.model.dto.file.DriveFileCountDto;
import com.GASB.google_drive_func.model.dto.file.DirveFileSizeDto;
import com.GASB.google_drive_func.model.dto.file.DriveRecentFileDTO;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.repository.files.FileUploadRepository;
import com.GASB.google_drive_func.model.repository.files.StoredFileRepository;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
import com.GASB.google_drive_func.service.DriveApiService;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DriveFileSerivce {

    public final DriveApiService driveApiService;
    public final FileUtil fileUtil;
    public final OrgSaaSRepo orgSaaSRepo;
    public final GoogleUtil googleUtil;
    public final WorkspaceConfigRepo workspaceConfigRepo;
    public final FileUploadRepository fileUploadRepository;
    public final StoredFileRepository storedFilesRepository;
    public final MonitoredUserRepo monitoredUserRepo;

    private static final List<String> spaceIdList = List.of("0ABuaF1uQELhqUk9PVA", "0ABfguJfTe9jEUk9PVA");
    @Autowired
    public DriveFileSerivce(DriveApiService driveApiService, FileUtil fileUtil, OrgSaaSRepo orgSaaSRepo
            , GoogleUtil googleUtil, WorkspaceConfigRepo worekSpaceRepo
            , StoredFileRepository storedFilesRepository, FileUploadRepository fileUploadRepository
            , MonitoredUserRepo monitoredUserRepo) {
        this.driveApiService = driveApiService;
        this.fileUtil = fileUtil;
        this.orgSaaSRepo = orgSaaSRepo;
        this.googleUtil = googleUtil;
        this.workspaceConfigRepo = worekSpaceRepo;
        this.storedFilesRepository = storedFilesRepository;
        this.fileUploadRepository = fileUploadRepository;
        this.monitoredUserRepo = monitoredUserRepo;
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


    public DriveFileCountDto FileCountSum(int orgId, int saasId) {
        return DriveFileCountDto.builder()
                .totalFiles(storedFilesRepository.countTotalFiles(orgId, saasId))
                .sensitiveFiles(storedFilesRepository.countSensitiveFiles(orgId, saasId))
                .maliciousFiles(storedFilesRepository.countMaliciousFiles(orgId, saasId))
                .connectedAccounts(storedFilesRepository.countConnectedAccounts(orgId, saasId))
                .build();
    }

    public List<DriveRecentFileDTO> DriveRecentFiles(int orgId, int saasId) {
        try {
            return fileUploadRepository.findRecentFilesByOrgIdAndSaasId(orgId, saasId);
        } catch (Exception e) {
            log.error("Error retrieving recent files for org_id: {} and saas_id: {}", orgId, saasId, e);
            return Collections.emptyList();
        }
    }

    @Async("threadPoolTaskExecutor")
    public CompletableFuture<List<TopUserDTO>> getTopUsersAsync(int orgId, int saasId) {
        return CompletableFuture.supplyAsync(() -> getTopUsers(orgId, saasId));
    }

    // 쿼리문 사용할때 네이티브 쿼리면 DTO에 직접 매핑시켜줘야함
    // JPQL이면 DTO에 매핑시켜줄 필요 없음
    public List<TopUserDTO> getTopUsers(int orgId, int saasId) {
        try {
            List<Object[]> results = monitoredUserRepo.findTopUsers(orgId, saasId);

            return results.stream().map(result -> new TopUserDTO(
                    (String) result[0],
                    ((Number) result[1]).longValue(),
                    ((Number) result[2]).longValue(),
                    ((java.sql.Timestamp) result[3]).toLocalDateTime()
            )).collect(Collectors.toList());

        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving top users", e);
        }
    }

    public DirveFileSizeDto sumOfFileSize(int orgId, int saasId) {
        return DirveFileSizeDto.builder()
                .totalSize((float) getTotalFileSize(orgId,saasId) / 1073741824)
                .sensitiveSize((float) getTotalDlpFileSize(orgId,saasId) / 1073741824)
                .maliciousSize((float) getTotalMaliciousFileSize(orgId,saasId) / 1073741824)
                .build();
    }

    public Long getTotalFileSize(int orgId, int saasId) {
        Long totalFileSize = storedFilesRepository.getTotalFileSize(orgId, saasId);
        return totalFileSize != null ? totalFileSize : 0L; // null 반환 방지
    }

    public Long getTotalMaliciousFileSize(int orgId, int saasId) {
        Long totalMaliciousFileSize = storedFilesRepository.getTotalMaliciousFileSize(orgId, saasId);
        return totalMaliciousFileSize != null ? totalMaliciousFileSize : 0L; // null 반환 방지
    }

    public Long getTotalDlpFileSize(int orgId, int saasId) {
        Long totalDlpFileSize = storedFilesRepository.getTotalDlpFileSize(orgId, saasId);
        return totalDlpFileSize != null ? totalDlpFileSize : 0L; // null 반환 방지
    }
}