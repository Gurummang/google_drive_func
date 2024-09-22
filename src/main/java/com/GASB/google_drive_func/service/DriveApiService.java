package com.GASB.google_drive_func.service;

import com.GASB.google_drive_func.model.repository.files.FileActivityRepo;
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
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DriveApiService {

    private final GoogleUtil googleUtil;
    private final FileActivityRepo fileActivityRepo;
    @Autowired
    public DriveApiService(GoogleUtil googleUtil, FileActivityRepo fileActivityRepo) {
        this.googleUtil = googleUtil;
        this.fileActivityRepo = fileActivityRepo;
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


    public List<Map<String,String>> getFileDetails(Drive service, String resource_uri) throws IOException {

        List<Map<String,String>> response_list = new ArrayList<>();

        // 페이지 토큰 추출
        String PageToken = extractPageToken(resource_uri);
        log.info("Page Token: {}", PageToken);
        // 파일 변경 이벤트 조회
        ChangeList changeList = service.changes().list(PageToken)
                .setPageSize(1)
                .execute();
        log.info("ChangeList: {}", changeList);
        if (changeList.getChanges().isEmpty()) {
            return null;
        }

        for (Change change : changeList.getChanges()){
            Map<String,String> response = new HashMap<>();
            log.info("Change: {}", change);
            if (isDuplicateLog(change)){
                log.info("Duplicate log: {}", change.getFileId());
                continue;
            }
            String event_Type = decideType(change);
            String file_id = change.getFileId();
            if (!event_Type.equals(null)){
                response.put("eventType",event_Type);
                response.put("fileId",file_id);
            } else{
                response.put("eventType","unknown");
                response.put("fileId",file_id);
            }
            response_list.add(response);

        }

        return response_list;
    }

    private String extractPageToken(String resourceUri) {
        if (resourceUri == null || resourceUri.isEmpty()) {
            return null; // URI가 null이거나 비어있을 경우 null 반환
        }

        try {
            // URI에서 '?' 이후의 쿼리 문자열 추출
            String query = resourceUri.split("\\?")[1];

            // '&'로 구분하여 각 쿼리 파라미터를 분리
            String[] params = query.split("&");

            // 각 파라미터에서 'pageToken'을 찾음
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2 && "pageToken".equals(keyValue[0])) {
                    return keyValue[1]; // pageToken 값 반환
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            // 쿼리 문자열이 없거나 형식이 잘못된 경우 예외 처리
            log.error("Error extracting pageToken: {}", e.getMessage());
        }

        return null; // pageToken을 찾지 못한 경우 null 반환
    }


    private boolean isDuplicateLog(Change changeFile){
        DateTime time = changeFile.getTime();

        //현재 시각
        DateTime now = new DateTime(System.currentTimeMillis());

        // 현재시각과 비교해서 1분 이내에 생성된 로그인지 확인
        return time.getValue() > now.getValue() - 60000;
    }

    private String decideType(Change changeFile){
        if (Boolean.TRUE.equals(changeFile.getRemoved())){
            return "delete";
        }
        // 파일 추가 or 파일 변경(수정)
        if (changeFile.getFile() == null) {
            log.error("File is null.");
            return null;
        }

        if (fileActivityRepo.existsBySaaSFileId(changeFile.getFileId())){
            return "update";
        } else {
            return "create";
        }
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
