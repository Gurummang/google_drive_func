package com.GASB.google_drive_func.service.file;

import com.GASB.google_drive_func.model.entity.FileUploadTable;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.repository.files.FileUploadRepository;
import com.GASB.google_drive_func.model.repository.files.StoredFileRepository;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
import com.GASB.google_drive_func.service.DriveApiService;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
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
    @Autowired
    public DriveFileService(DriveApiService driveApiService, FileUtil fileUtil, OrgSaaSRepo orgSaaSRepo
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

    public boolean fileDelete(int idx, String fileHash) {
        try {
            // 파일 ID와 해시값을 통해 파일 조회
            FileUploadTable targetFile = fileUploadRepository.findByIdAndFileHash(idx, fileHash).orElse(null);
            if (targetFile == null) {
                log.error("File not found or invalid: id={}, hash={}", idx, fileHash);
                return false;
            }
            // 해당 파일이 Slack 파일인지 확인
            if (orgSaaSRepo.findSaaSIdById(targetFile.getOrgSaaS().getId()) != 6) {
                log.error("File is not a Slack file: id={}, saasId={}", idx, targetFile.getOrgSaaS().getId());
                return false;
            }
            // Slack API를 통해 파일 삭제 요청
            return driveApiService.DriveFileDeleteApi(targetFile.getOrgSaaS().getId(), targetFile.getSaasFileId());

        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("Error deleting file: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Error deleting file: {}", e.getMessage());
            return false;
        }
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

    public void deleteAllWatch() throws Exception {
        // 드라이브 서비스 가져오기
        Drive service = googleUtil.getDriveService(231);

        // null 체크
        if (service == null) {
            throw new Exception("Drive service is null");
        }

        // 채널 ID 리스트 초기화
        List<String> channelIds = new ArrayList<>();
        channelIds.add("19f9a220-782a-4a45-8c8f-4c0d3927d3df");

        // 리소스 ID 설정
        String resourceId = "8v4U7LlTVAH2h60NhO4RybdKoAY";

        // 채널 ID 리스트가 비어 있는지 확인
        if (channelIds.isEmpty()) {
            System.out.println("No channels to stop.");
            return;
        }

        // 각 채널에 대해 중지 작업 수행
        for (String channelId : channelIds) {
            if (channelId != null && !channelId.isEmpty()) {
                try {
                    // 채널 중지 메소드 호출
                    Channel channel = new Channel();
                    channel.setId(channelId);
                    channel.setResourceId(resourceId);

                    service.channels().stop(channel).execute();
                    System.out.println("Successfully stopped channel: " + channelId);
                } catch (IOException e) {
                    // 예외 발생 시 로깅
                    System.err.println("Failed to stop channel: " + channelId + ", reason: " + e.getMessage());
                }
            } else {
                System.out.println("Invalid channelId: " + channelId);
            }
        }
    }


}