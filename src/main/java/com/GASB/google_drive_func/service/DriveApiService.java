package com.GASB.google_drive_func.service;

import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.*;
import com.rometools.utils.IO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DriveApiService {

    private final GoogleUtil googleUtil;
    @Autowired
    public DriveApiService(GoogleUtil googleUtil) {
        this.googleUtil = googleUtil;
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


    public Map<String,String> getFileDetails(Drive service, String resource_uri) throws IOException {
        Map<String,String> response = new HashMap<>();
        // 페이지 토큰 추출
        String PageToken = extractPageToken(resource_uri);
        log.info("Page Token: {}", PageToken);
        // 파일 변경 이벤트 조회
        ChangeList changeList = service.changes().list(PageToken)
                .setPageSize(1)
                .setFields("changes(fileId,removed,file(id,name,trashed,createdTime,modifiedTime,mimeType)),newStartPageToken")
                .execute();
        log.info("ChangeList: {}", changeList);
        if (changeList.getChanges().isEmpty()) {
            return null;
        }

        Change change = changeList.getChanges().get(0);
        log.info("Change: {}", change);
        String fileId = change.getFileId();
        log.info("File ID: {}", fileId);

        File file = service.files().get(fileId)
                .setFields("id, name, trashed, explicitlyTrashed, createdTime, modifiedTime, mimeType")
                .execute();

        // 파일이 삭제된 상태인지 확인
        if (Boolean.TRUE.equals(change.getRemoved()) || Boolean.TRUE.equals(file.getTrashed()) || Boolean.TRUE.equals(file.getExplicitlyTrashed()) ){
            response.put("eventType","delete");
            response.put("fileId",fileId);
        }

        // 파일이 새로 생성된 상태인지 확인 (현재 시각과 비교)
        File changeFile = change.getFile();
        if (changeFile == null) {
            return null;
        }

        if (Boolean.TRUE.equals(changeFile.getTrashed())) {
            response.put("eventType","delete");
            response.put("fileId",fileId);
            log.info("Response: {}", response);
            return response;
        }
        String eventType = isNewOrModified(changeFile);
        if (eventType.equals(null)){
            response.put("eventType","unknown");
            response.put("fileId",fileId);
            log.info("Response: {}", response);
            return response;
        }
        response.put("eventType",eventType);
        response.put("fileId",fileId);
        log.info("Response: {}", response);
        return response;
    }

    private String extractPageToken(String resource_uri) {
        log.info("extractPageToken to {}", resource_uri);
        if (resource_uri == null || resource_uri.isEmpty()) {
            log.error("Resource URI is null or empty.");
            return null;
        }

        String[] uriParts = resource_uri.split("=");
        if (uriParts.length < 2) {
            log.error("Invalid resource URI format: {}", resource_uri);
            return null;
        }

        return uriParts[uriParts.length - 1];
    }

    private String isNewOrModified(File file) {
        if (file == null) {
            log.error("File is null.");
            return "unknown"; // 파일이 null인 경우
        }

        try {
            Instant createdTime = Instant.ofEpochMilli(file.getCreatedTime().getValue());
            Instant modifiedTime = Instant.ofEpochMilli(file.getModifiedTime().getValue());

            if (modifiedTime == null) {
                log.warn("Modified time is null for file: {}", file.getId());
                return "new"; // 수정 시간이 없는 경우 새로 생성된 것으로 간주
            }

            if (createdTime.isAfter(modifiedTime)) {
                log.info("File is new: {}", file.getId());
                return "new";
            } else {
                log.info("File is modified: {}", file.getId());
                return "modified";
            }
        } catch (NullPointerException e) {
            log.error("Error processing file: {}, message: {}", file != null ? file.getId() : "unknown", e.getMessage());
            return "unknown"; // 예외 발생 시
        }
    }




}
