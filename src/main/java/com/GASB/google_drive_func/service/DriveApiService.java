package com.GASB.google_drive_func.service;

import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.repository.channel.GooglePageTokenRepo;
import com.GASB.google_drive_func.model.repository.files.FileActivityRepo;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.*;
import com.rometools.utils.IO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
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

    public File fetchOneFile(String fileId, int workspaceId, Drive service) throws Exception {

        try {
            return service.files().get(fileId)
                    .setSupportsAllDrives(true)
                    .setFields("id, name, mimeType, size, createdTime, modifiedTime, parents, webViewLink, iconLink, thumbnailLink, permissions(id, type, emailAddress, role), lastModifyingUser(displayName, emailAddress)")
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
                    .setFields("permissions(id,emailAddress,displayName,role)")
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

    public boolean DriveFileDeleteApi(int id, String driveFileId) {
        try {
            // Google Drive 파일 삭제 API 호출
            Drive service = googleUtil.getDriveService(id);
            service.files().delete(driveFileId).execute(); // 파일 ID로 삭제 요청
            return true; // 삭제 성공 시 true 반환

        } catch (GoogleJsonResponseException e) {
            // Google API 에러 처리
            log.error("Google Drive API Error: " + e.getDetails().getMessage());
            return false;
        } catch (Exception e) {
            // 네트워크/I/O 에러 처리
            log.error("Error occurred: " + e.getMessage());
            return false;
        }
    }
        // 단일 파일의 변경 상태 조회


    public List<Map<String,String>> getFileDetails(Drive service, String channel_id) throws IOException {

        List<Map<String,String>> response_list = new ArrayList<>();

        // 페이지 토큰 추출
        String pageToken = googlePageTokenRepo.getPageTokenByChannelId(channel_id).orElse(null);
        if (pageToken == null) {
            log.error("Page token not found for channel ID: {}", channel_id);
            return null;
        }
        log.info("Page Token: {}", pageToken);


        // 파일 변경 이벤트 조회
        ChangeList changeList = service.changes().list(pageToken).execute();
        log.info("ChangeList: {}", changeList);
        if (changeList.getChanges().isEmpty()) {
            return null;
        }

        for (Change change : changeList.getChanges()){
            Map<String,String> response = new HashMap<>();
            log.info("Change: {}", change);
            if (isDuplicateLog(change) || change.getChangeType().equals("drive")){
                log.info("Change Event is dir or duplicate log: {}", change.getChangeType());
                log.info("Duplicate log: {}", change.getFileId());
                continue;
            }
            String event_Type = decideType(change);
            String file_id = change.getFileId();
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
        int org_saas_id = googlePageTokenRepo.findByChannelId(channel_id);
        OrgSaaS orgSaaSObj = orgSaaSRepo.findById(org_saas_id).orElseThrow(() -> new IllegalStateException("Workspace not found"));
        String newPageToken = service.changes().getStartPageToken()
                .setDriveId(orgSaaSObj.getSpaceId())
                .setSupportsAllDrives(true)
                .execute()
                .getStartPageToken();
        log.info("New Page Token: {}", newPageToken);
        googlePageTokenRepo.updatePageTokenByChannelId(channel_id, newPageToken);



        return response_list;
    }

    private boolean isDuplicateLog(Change changeFile) {
        if (changeFile.getTime() == null) {
            // 시간 정보가 없는 경우 처리
            return false; // 또는 적절한 기본값 반환
        }

        // Change의 시간 정보를 Instant로 변환
        Instant changeInstant = Instant.ofEpochMilli(changeFile.getTime().getValue());

        // 한국 시간대 (UTC+9:00)로 변환
        ZoneId koreaZoneId = ZoneId.of("Asia/Seoul");
        ZonedDateTime changeTime = changeInstant.atZone(koreaZoneId);
        log.info("Change time: {}", changeTime);
        // 현재 시각을 한국 시간대로 설정
        ZonedDateTime now = ZonedDateTime.now(koreaZoneId);
        log.info("Current time: {}", now);

        // 두 시간 사이의 차이를 초 단위로 계산
        long secondsBetween = ChronoUnit.SECONDS.between(changeTime, now);
        log.info("Seconds between: {}", secondsBetween);

        // 30초 이내인지 확인
        return secondsBetween >= 30;
    }


    private String decideType(Change changeFile) {
        if (Boolean.TRUE.equals(changeFile.getRemoved()) || Boolean.TRUE.equals(changeFile.getFile().getTrashed())) {
            return "delete";
        }
        // 파일 추가 or 파일 변경(수정)
        if (changeFile.getFile() == null) {
            log.error("File is null.");
            return null;
        }

        if (fileActivityRepo.existsBySaaSFileId(changeFile.getFileId())) {
            return "update";
        } else {
            return "create";
        }
    }
}
