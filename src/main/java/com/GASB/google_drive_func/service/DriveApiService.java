package com.GASB.google_drive_func.service;

import com.GASB.google_drive_func.model.entity.GooglePageToken;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.repository.channel.GooglePageTokenRepo;
import com.GASB.google_drive_func.model.repository.files.FileActivityRepo;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DriveApiService {

    private final GoogleUtil googleUtil;
    private final FileActivityRepo fileActivityRepo;
    private final GooglePageTokenRepo googlePageTokenRepo;
    private final OrgSaaSRepo orgSaaSRepo;
    @Autowired
    public DriveApiService(GoogleUtil googleUtil, FileActivityRepo fileActivityRepo, GooglePageTokenRepo googlePageTokenRepo, OrgSaaSRepo orgSaaSRepo) {
        this.googleUtil = googleUtil;
        this.fileActivityRepo = fileActivityRepo;
        this.googlePageTokenRepo = googlePageTokenRepo;
        this.orgSaaSRepo = orgSaaSRepo;
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
        } catch (NullPointerException e) {
            log.error("Error fetching files: {}", e.getMessage());
            return null;
        } catch (IOException e) {
            log.error("IO error while fetching files: {}", e.getMessage());
            return null;
        }
    }

    public File fetchOneFile(String fileId, Drive service) throws Exception {

        try {
            return service.files().get(fileId)
                    .setSupportsAllDrives(true)
                    .setFields("id, name, mimeType,fileExtension, mimeType , size, createdTime, trashed, modifiedTime, parents, permissions(id, type, emailAddress, role), lastModifyingUser(displayName, emailAddress, permissionId)")
                    .execute();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("File not found: {}", fileId);
                return null;
            }
            log.error("Error fetching file {}: {}", fileId, e.getMessage());
            throw e;
        } catch (IOException e) {
            log.error("IO error while fetching file {}: {}", fileId, e.getMessage());
            throw e;
        }
    }

    public PermissionList fetchUser(Drive service, String DriveId) {
        try {
            return service.permissions().list(DriveId)
                    .setSupportsAllDrives(true)
                    .setUseDomainAdminAccess(true)
                    .setFields("permissions(id,emailAddress,displayName,role, deleted)")
                    .execute();
        } catch (IOException e) {
            log.error("Error fetching users: {}", e.getMessage());
            return null;
        } catch (NullPointerException e) {
            log.error("Error fetching users: {}", e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error fetching users: {}", e.getMessage());
            return null;
        }
    }

    public boolean DriveFileDeleteApi(int id, String driveFileId) throws IOException {
        try {
            Drive service = googleUtil.getDriveService(id);

            if (Boolean.FALSE.equals(isFileExist(driveFileId, service))) {
                log.warn("File not found: " + driveFileId);
                return false;
            } else {
                log.info("File found: " + driveFileId);
            }
            // 파일 삭제 (공유 드라이브 지원 포함)
            service.files().delete(driveFileId)
                            .setSupportsAllDrives(true)
                                    .execute();

            log.info("File deleted successfully: " + driveFileId);
            return true;
        } catch (GoogleJsonResponseException e) {
            log.error("Google Drive API Error: " + e.getDetails().toPrettyString());
            return false;
        } catch (Exception e) {
            log.error("Error occurred: " + e.getMessage());
            return false;
        }
    }
        // 단일 파일의 변경 상태 조회

    private boolean isFileExist(String fileId, Drive service) {
        try {

            File file = service.files().get(fileId).setSupportsAllDrives(true).execute();
            if (file == null) {
                log.warn("File not found: {}", fileId);
                return false;
            }
            return true;
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("File not found: {}", fileId);
                return false;
            }
            log.error("Error checking if file {} exists: {}", fileId, e.getDetails().getMessage());
            return false;
        } catch (IOException e) {
            log.error("Error checking if file {} exists: {}", fileId, e.getMessage());
            return false;
        }
    }
    public List<Map<String,String>> getFileDetails(Drive service, String channel_id) throws IOException {

        List<Map<String,String>> response_list = new ArrayList<>();
        GooglePageToken pageTokenObj = googlePageTokenRepo.findObjByChannelId(channel_id).orElseThrow(() -> new IllegalStateException("Page Token not found"));
        OrgSaaS orgSaaSObj = pageTokenObj.getOrgSaaS();

        // 페이지 토큰 추출
        String pageToken = pageTokenObj.getPageToken();
        if (pageToken == null) {
            log.error("Page token not found for channel ID: {}", channel_id);
            return null;
        }
        log.info("Page Token: {}", pageToken);


        // 파일 변경 이벤트 조회
        ChangeList changeList = service.changes()
                .list(pageToken)
                .setDriveId(orgSaaSObj.getSpaceId())
                .setIncludeRemoved(true)
                .setSupportsAllDrives(true)
                .setIncludeItemsFromAllDrives(true)
                .setIncludeCorpusRemovals(true)
                .execute();

        log.info("ChangeList: {}", changeList);
        if (changeList.getChanges().isEmpty()) {
            log.info("No changes found.");
            return null;
        }

        for (Change change : changeList.getChanges()){
            Map<String,String> response = new HashMap<>();
            log.info("Change: {}", change);
            if (change.getChangeType()!=null && change.getChangeType().equals("drive")){
                log.info("Change Event is not file, detail : {}", change.getChangeType());
                if (pageTokenObj.getLastAccessTime() != null){
                    if (isDuplicateLog(change, pageTokenObj.getLastAccessTime())){
                        log.info("Duplicate log: {}", change.getFileId());
                        continue;
                    }
                }
                continue;
            }
            // 중복 로그가 아니라면 마지막 엑세스 시간 업데이트
            googlePageTokenRepo.updateLastAccessTimeByChannelId(channel_id);

            String event_Type = decideType(change, service);

            String file_id = change.getFileId();
            log.info("Successfully decided type: {}, file_id : {}", event_Type, file_id);
            if (event_Type != null){
                response.put("eventType",event_Type);
                response.put("fileId",file_id);
            } else{
                response.put("eventType","unknown");
                response.put("fileId",file_id);
            }
            response_list.add(response);

        }
        // 페이지 토큰 업데이트

        String newPageToken = service.changes().getStartPageToken()
                .setDriveId(orgSaaSObj.getSpaceId())
                .setSupportsAllDrives(true)
                .execute()
                .getStartPageToken();
        log.info("New Page Token: {}", newPageToken);
        googlePageTokenRepo.updatePageTokenByChannelId(channel_id, newPageToken);



        return response_list;
    }

    private boolean isDuplicateLog(Change changeFile, LocalDateTime lastAccessTime) {
        if (changeFile.getTime() == null) {
            // 시간 정보가 없는 경우 처리
            return false; // 또는 적절한 기본값 반환
        }

        // Change의 시간 정보를 Instant로 변환
        Instant changeInstant = Instant.ofEpochMilli(changeFile.getTime().getValue());

        // 한국 시간대 (UTC+9:00)로 변환
        ZoneId koreaZoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime changeTime = changeInstant.atZone(koreaZoneId);

        // lastAccessTime을 한국 시간으로 변경
        ZonedDateTime lastAccess = lastAccessTime.atZone(koreaZoneId);



        // 로그에 있는 시간과 마지막 엑세스 시간 비교
        long secondsBetween = ChronoUnit.SECONDS.between(changeTime, lastAccess);
        log.info("Change time : {} , Last access time : {}, Seconds between: {}", changeTime, lastAccess, secondsBetween);

        // 30초 이내인지 확인
        return secondsBetween <= 30;
    }


    private String decideType(Change changeFile, Drive service) throws IOException {
        // 파일 추가 or 파일 변경(수정)
        if (changeFile.getFile() == null) {
            log.error("File is null.");
            return null;
        }

        if (fileActivityRepo.existsBySaaSFileId(changeFile.getFileId())) {
            if (Boolean.TRUE.equals(changeFile.getRemoved()) || Boolean.TRUE.equals(isFileTrashed(changeFile.getFileId(), service))) {
                return "delete";
            } else {
                return "update";
            }
        } else {
            return "create";
        }
    }

    private boolean isFileTrashed(String fileId, Drive service) throws IOException {
        try {
            File file = service.files().get(fileId)
                    .setSupportsAllDrives(true)
                    .setFields("trashed")
                    .execute();
            return file.getTrashed();
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 404) {
                log.warn("File not found: {}", fileId);
                return false;
            }
            log.error("Error checking if file {} is trashed: {}", fileId, e.getDetails().getMessage());
            throw e;
        }
    }
}
