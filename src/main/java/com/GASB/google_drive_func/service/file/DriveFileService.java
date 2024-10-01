package com.GASB.google_drive_func.service.file;

import com.GASB.google_drive_func.model.entity.Activities;
import com.GASB.google_drive_func.model.entity.FileUploadTable;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.repository.files.FileActivityRepo;
import com.GASB.google_drive_func.model.repository.files.FileUploadRepository;
import com.GASB.google_drive_func.model.repository.files.StoredFileRepository;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
import com.GASB.google_drive_func.service.DriveApiService;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.drive.model.File;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class DriveFileService {

    public final DriveApiService driveApiService;
    public final FileUtil fileUtil;
    public final OrgSaaSRepo orgSaaSRepo;
    public final GoogleUtil googleUtil;
    public final WorkspaceConfigRepo workspaceConfigRepo;
    public final FileUploadRepository fileUploadRepository;
    public final StoredFileRepository storedFilesRepository;
    public final MonitoredUserRepo monitoredUserRepo;
    private final FileActivityRepo fileActivityRepo;
    @Autowired
    public DriveFileService(DriveApiService driveApiService, FileUtil fileUtil, OrgSaaSRepo orgSaaSRepo
            , GoogleUtil googleUtil, WorkspaceConfigRepo worekSpaceRepo
            , StoredFileRepository storedFilesRepository, FileUploadRepository fileUploadRepository
            , MonitoredUserRepo monitoredUserRepo, FileActivityRepo fileActivityRepo) {
        this.driveApiService = driveApiService;
        this.fileUtil = fileUtil;
        this.orgSaaSRepo = orgSaaSRepo;
        this.googleUtil = googleUtil;
        this.workspaceConfigRepo = worekSpaceRepo;
        this.storedFilesRepository = storedFilesRepository;
        this.fileUploadRepository = fileUploadRepository;
        this.monitoredUserRepo = monitoredUserRepo;
        this.fileActivityRepo = fileActivityRepo;
    }

    public void fileDelete(List<Map<String, String>> requests) {
        // 입력 값 null 체크
        if (requests == null || requests.isEmpty()) {
            log.error("Request list is null or empty");
            return;
        }

        try {
            for (Map<String, String> request : requests) {
                // 개별 request에 대한 null 체크
                if (request == null || request.get("id") == null || request.get("file_name") == null) {
                    log.error("Invalid request: null values detected");
                    continue; // 해당 요청 건을 건너뜀
                }

                int idx = Integer.parseInt(request.get("id"));
                String file_name = request.get("file_name");

                // 파일 체크
                if (!checkFile(idx, file_name)) {
                    log.error("File not found or invalid: id={}, name={}", idx, file_name);
                    continue; // 해당 요청 건을 건너뜀
                }

                // 파일 ID와 해시값을 통해 파일 조회
                FileUploadTable targetFile = fileUploadRepository.findById(idx).orElse(null);
                if (targetFile == null) {
                    log.error("File not found with id: {}", idx);
                    continue; // 파일이 존재하지 않는 경우 다음 루프로 진행
                }

                // google drive API를 통해 파일 삭제 요청
                try {
                    driveApiService.DriveFileDeleteApi(targetFile.getOrgSaaS().getId(), targetFile.getSaasFileId());
                    log.info("Successfully deleted file: id={}, name={}", idx, file_name);
                } catch (IOException e) {
                    log.error("Error deleting file via Google Drive API: id={}, name={}. Exception: {}", idx, file_name, e.getMessage());
                    throw new RuntimeException("Google Drive API failed", e); // 적절한 예외로 래핑
                }
            }
        } catch (NumberFormatException e) {
            log.error("Invalid file id format: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("Error deleting file: {}", e.getMessage());
        }
    }


    private boolean checkFile(int idx, String file_name){

        FileUploadTable targetFile = fileUploadRepository.findById(idx).orElse(null);
        Activities targetFileActivity = fileActivityRepo.findBySaasFileId(Objects.requireNonNull(targetFile).getSaasFileId()).orElse(null);
        String tmp_file_name = Objects.requireNonNull(targetFileActivity).getFileName();
        if (!tmp_file_name.equals(file_name)) {
            log.error("File name not matched: id={}, name={}", idx, file_name);
            return false;
        }
        if (orgSaaSRepo.findSaaSIdById(targetFile.getOrgSaaS().getId()) !=6) {
            log.error("File is not a GoogleDrive file: id={}, saasId={}", idx, targetFile.getOrgSaaS().getId());
            return false;
        }
        return true;
    }
    @Transactional
    public CompletableFuture<Void> fetchAndStoreFiles(int workspaceId, String eventType) {
        return CompletableFuture.runAsync(() -> {
            try {
                Drive service = googleUtil.getDriveService(workspaceId);
                OrgSaaS orgSaaSObject = orgSaaSRepo.findById(workspaceId).orElse(null);

                String spaceId = orgSaaSRepo.getSpaceID(workspaceId);
                List<File> fileList = driveApiService.fetchFiles(service, spaceId).getFiles();

                List<CompletableFuture<Void>> futures = new ArrayList<>();
                log.info("Processing {} files", fileList.size());
                log.info("FileList : {}", fileList);
                for (File file : fileList) {
                    if (shouldSkipFile(file)) {
                        log.info("Skipping unsupported file: {}, {}", file.getId(), file.getName());
                        continue;
                    }
                    CompletableFuture<Void> future = fileUtil.processAndStoreFile(file, orgSaaSObject, workspaceId, eventType, service);
                    futures.add(future);
                }

                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            } catch (IllegalArgumentException e) {
                log.error("Error fetching files: {}", e.getMessage());
            } catch (Exception e) {
                log.error("Error fetching files: {}", e.getMessage());
            }
        });
    }


    private boolean shouldSkipFile(File file) {
        return "application/vnd.google-apps.spreadsheet".equalsIgnoreCase(file.getMimeType()) ||  // Google Sheets
                "application/vnd.google-apps.document".equalsIgnoreCase(file.getMimeType()) ||    // Google Docs
                "application/vnd.google-apps.presentation".equalsIgnoreCase(file.getMimeType());  // Google Slides
    }

    public void deleteAllWatch(int workspaceId, String channelId, String driveId) throws Exception {
        // 드라이브 서비스 가져오기
        Drive service = googleUtil.getDriveService(workspaceId);

        // null 체크
        if (service == null) {
            throw new IllegalArgumentException("Drive service is null for workspace ID: " + workspaceId);
        }

        if (channelId == null || channelId.isEmpty()) {
            throw new IllegalArgumentException("Channel ID must not be null or empty");
        }

        if (driveId == null || driveId.isEmpty()) {
            throw new IllegalArgumentException("Drive ID must not be null or empty");
        }

        try {
            // 채널 설정 및 삭제 요청
            Channel channel = new Channel();
            channel.setId(channelId);
            channel.setResourceId(driveId);

            service.channels().stop(channel).execute();

            // 성공 로그
            log.info("Successfully stopped channel");

        } catch (GoogleJsonResponseException e) {
            // Google API 호출 시 발생할 수 있는 예외 처리
            log.error("Google API error while stopping channel: {}", e.getDetails(), e);
            throw new Exception("Failed to stop the channel due to Google API error", e);

        } catch (IOException e) {
            // IO 오류 처리
            log.error("I/O error while stopping channel: {}", e.getMessage(), e);
            throw new Exception("Failed to stop the channel due to an I/O error", e);

        } catch (Exception e) {
            // 기타 예외 처리
            log.error("Unexpected error while stopping channel: {}", e.getMessage(), e);
            throw new Exception("Failed to stop the channel due to an unexpected error", e);
        }
    }


}