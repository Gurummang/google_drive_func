package com.GASB.google_drive_func.service.file;

import com.GASB.google_drive_func.model.dto.TopUserDTO;
import com.GASB.google_drive_func.model.dto.file.DirveFileSizeDto;
import com.GASB.google_drive_func.model.dto.file.DriveFileCountDto;
import com.GASB.google_drive_func.model.dto.file.DriveRecentFileDTO;
import com.GASB.google_drive_func.model.entity.*;
import com.GASB.google_drive_func.model.mapper.DriveFileMapper;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
import com.GASB.google_drive_func.model.repository.files.FileActivityRepo;
import com.GASB.google_drive_func.model.repository.files.FileUploadRepository;
import com.GASB.google_drive_func.model.repository.files.StoredFileRepository;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.GASB.google_drive_func.service.FileEncUtil;
import com.GASB.google_drive_func.service.event.MessageSender;
import com.GASB.google_drive_func.tlsh.Tlsh;
import com.GASB.google_drive_func.tlsh.TlshCreator;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import javax.annotation.PostConstruct;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.nimbusds.openid.connect.sdk.id.HashBasedPairwiseSubjectCodec.HASH_ALGORITHM;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileUtil {

    private final WorkspaceConfigRepo worekSpaceRepo;
    private final StoredFileRepository storedFilesRepository;
    private final DriveFileMapper driveFileMapper;
    private final MonitoredUserRepo monitoredUserRepo;
    private final FileUploadRepository fileUploadRepository;
    private final FileActivityRepo activitiesRepository;
    private final S3Client s3Client;
    private final ScanUtil scanUtil;
    private final MessageSender messageSender;
    private final FileEncUtil fileEncUtil;

    private static final Path BASE_PATH = Paths.get("downloads");

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(BASE_PATH);
            log.info("Base path directory created or already exists: {}", BASE_PATH.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to create base path directory: {}", BASE_PATH.toAbsolutePath(), e);
            throw new RuntimeException("Could not create base directory", e);
        }
    }
    @Value("${aws.s3.bucket}")
    private String bucketName;

    private List<String> getParentId(File file, Drive service) {
        List<String> parents = new ArrayList<>();
        String parentId = file.getParents() != null && !file.getParents().isEmpty() ? file.getParents().get(0) : null;

        while (parentId != null) {
            try {
                File parentFile = service.files().get(parentId)
                        .setFields("id, name, parents")
                        .setSupportsAllDrives(true)
                        .execute();
                log.info("Parent file: {}", parentFile);
                parents.add(0, parentFile.getName());  // 상위 경로 이름을 맨 앞에 추가
                parentId = parentFile.getParents() != null && !parentFile.getParents().isEmpty() ? parentFile.getParents().get(0) : null;
            } catch (IOException e) {
                log.error("Failed to get parent file", e);
                break;
            }
        }
        return parents;
    }

    private String buildPath(File file, String SaaSName, String orgName, String DriveName, List<String> parents) {
        // 파일 이름을 경로에 추가
        parents.add(file.getName());

        // "Drive" 문자열이 존재하면 DriveName으로 변경
        int driveIndex = parents.indexOf("Drive");
        if (driveIndex != -1) {
            parents.set(driveIndex, DriveName);
        }

        // 드라이브 이름과 조직명을 맨 앞에 추가
        parents.add(0, SaaSName);
        parents.add(0, orgName);

        return String.join("/", parents);
    }

    public String getFullPath(File file, String SaaSName, String orgName, String hash, String DriveName, List<String> parents) {
        // 해시 값을 경로에 추가
        if (parents == null) {
            return buildPath(file, SaaSName, orgName, DriveName, new ArrayList<>());
        }
//        List<String> tmpArray = new ArrayList<>(parents); // parents 리스트 복사
        parents.add(hash); // 해시 값을 경로에 추가

        return buildPath(file, SaaSName, orgName, DriveName, parents);
    }

    public String getDisplayPath(File file, String SaaSName, String orgName, String DriveName, List<String> parents) {
        if (parents == null) {
            return buildPath(file, SaaSName, orgName, DriveName, new ArrayList<>());
        }
//        List<String> tmpArray = new ArrayList<>(parents); // parents 리스트 복사
        return buildPath(file, SaaSName, orgName, DriveName, parents);
    }



    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            hexString.append(hex.length() == 1 ? "0" : "").append(hex);
        }
        return hexString.toString();
    }

    public String calculateHash(byte[] fileData) throws NoSuchAlgorithmException {
        // 널 체크
        if (fileData == null) {
            throw new IllegalArgumentException("fileData cannot be null");
        }

        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hash = digest.digest(fileData);

        return bytesToHex(hash);
    }


    //TLSH 해시 계산
    private Tlsh computeTlsHash(byte[] fileData) {
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("fileData cannot be null or empty");
        }

        final int BUFFER_SIZE = 4096;
        TlshCreator tlshCreator = new TlshCreator();
        if (tlshCreator == null) {
            log.error("TLSH creator is null");
            return null;
        }

        try (InputStream is = new ByteArrayInputStream(fileData)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buf)) != -1) {
                // buf 자체의 null 체크는 불필요, 내부적으로 초기화된 배열
                tlshCreator.update(buf, 0, bytesRead);
            }
        } catch (IOException e) {
            log.error("Error reading file data for TLSH hash calculation", e);
            return null; // TLSH 계산 실패 시 null 반환
        }

        try {
            return tlshCreator.getHash();
        } catch (IllegalStateException e) {
            log.warn("TLSH not valid; either not enough data or data has too little variance");
            return null; // TLSH 계산 실패 시 null 반환
        }
    }

    @Async("threadPoolTaskExecutor")
    @Transactional
    public CompletableFuture<Void> processAndStoreFile(File file, OrgSaaS orgSaaSObject, int workspaceId, String event_type, Drive service) {
        return downloadFileAsync(file, service)
                .thenApply(fileData -> {
                    try {
                        return handleFileProcessing(file, orgSaaSObject, fileData, workspaceId, event_type, service);
                    } catch (IOException | NoSuchAlgorithmException e) {
                        throw new RuntimeException("File processing failed", e);
                    }
                })
                .exceptionally(ex -> {
                    log.error("Error processing file: {}", file.getName(), ex);
                    return null;
                });
    }



    @Async("threadPoolTaskExecutor")
    public CompletableFuture<byte[]> downloadFileAsync(File file, Drive service) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return DownloadFileMethod(file.getId(), BASE_PATH.resolve(file.getName()).toString(), service);
            } catch (Exception e) {
                log.error("Unexpected error while downloading file {}: {}", file.getName(), e.getMessage());
                throw new RuntimeException("Unexpected error", e);
            }
        });
    }

    public byte[] DownloadFileMethod(String fileId, String filePath, Drive service) {
        try {
            // Google Drive 파일 메타데이터 가져오기
            com.google.api.services.drive.model.File file = service.files().get(fileId)
                    .setSupportsAllDrives(true)
                    .setFields("id, name, size, mimeType")
                    .execute();

            log.info("Downloading file: {} (ID: {})", file.getName(), file.getId());
            log.info("File size: {} bytes, MIME type: {}", file.getSize(), file.getMimeType());

            // 파일 저장 디렉터리 생성
            Path parentDir = Paths.get(filePath).getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 절대 경로로 변환하여 출력
            Path absolutePath = Paths.get(filePath).toAbsolutePath();
            log.info("Saving file to absolute path: {}", absolutePath);

            // 바이트 배열로 데이터를 다운로드 및 저장
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                 FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {

                service.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                outputStream.writeTo(fileOutputStream);

                byte[] fileData = outputStream.toByteArray();
                long downloadedSize = fileData.length;

                // 다운로드된 파일 크기 검증
                if (file.getSize() != null && downloadedSize == file.getSize()) {
                    log.info("Download verified: File size matches ({} bytes)", downloadedSize);
                    log.info("Download Successful, FileName: {}, File SavePath: {}", file.getName(), absolutePath);
                } else {
                    log.warn("Download size mismatch: Expected {} bytes, got {} bytes", file.getSize(), downloadedSize);
                }

                return fileData;

            } catch (IOException e) {
                log.error("IO error while downloading file: {}", e.getMessage(), e);
                throw new RuntimeException("File download failed", e);
            }

        } catch (IOException e) {
            log.error("IO error while downloading file: {}", e.getMessage(), e);
            throw new RuntimeException("File download failed", e);
        } catch (Exception e) {
            log.error("An unexpected error occurred while downloading the file: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error", e);
        }
    }


    private Void handleFileProcessing(File file, OrgSaaS orgSaaSObject, byte[] fileData, int workspaceId, String event_type, Drive service) throws IOException, NoSuchAlgorithmException {
        String hash = calculateHash(fileData);
        String tlsh = computeTlsHash(fileData).toString();
        WorkspaceConfig config = worekSpaceRepo.findById(workspaceId).orElse(null);
        String workspaceName = Objects.requireNonNull(config).getWorkspaceName();

        String saasname = orgSaaSObject.getSaas().getSaasName();
        String OrgName = orgSaaSObject.getOrg().getOrgName();
        List<String> parentsList = getParentId(file, service);
        List<String> parentsList2 = getParentId(file, service);
        String s3UploadPath = getFullPath(file, saasname, OrgName, hash, workspaceName, parentsList);
        log.info("File path: {}", s3UploadPath);
        String savedPath = getDisplayPath(file, saasname, OrgName, workspaceName, parentsList2);
        log.info("File saved path: {}", savedPath);
        String filePath = BASE_PATH.resolve(file.getName()).toString();


        MonitoredUsers user = monitoredUserRepo.fineByUserId(file.getLastModifyingUser().getPermissionId(),workspaceId).orElse(null);
        if (user == null){
            log.error("User not found in MonitoredUsers: {}", file.getLastModifyingUser().getPermissionId());
            return null;
        }
        StoredFile storedFileObj = driveFileMapper.toStoredFileEntity(file, hash, s3UploadPath);
        if (storedFileObj == null){
            log.error("StoredFile object is null");
            return null;
        }
        FileUploadTable fileUploadTableObj = driveFileMapper.toFileUploadEntity(file, orgSaaSObject, hash, event_type.equals("file_change"));
        if (fileUploadTableObj == null){
            log.error("FileUploadTable object is null");
            return null;
        }
        Activities activities = driveFileMapper.toActivityEntity(file, event_type, user, savedPath, tlsh);
        if (activities == null){
            log.error("Activities object is null");
            return null;
        }
        synchronized (this) {
            try {
                String file_name = file.getName();
                if (file_name == null){
                    log.error("File name is null");
                    return null;
                }
                if(storedFileObj.getSaltedHash() == null){
                    log.error("Salted hash is null");
                    return null;
                }
                if (!storedFilesRepository.existsBySaltedHash(storedFileObj.getSaltedHash())) {
                    storedFilesRepository.save(storedFileObj);
                    log.info("File uploaded successfully stored_file table: {}", file.getName());
                } else {
                    log.warn("Duplicate file detected in StoredFile: {}", file.getName());
                }

                if (fileUploadTableObj.getSaasFileId() == null){
                    log.error("Saas file id is null");
                    return null;
                }
                if (fileUploadTableObj.getTimestamp() == null){
                    log.error("Timestamp is null");
                    return null;
                }

                if (!fileUploadRepository.existsBySaasFileIdAndTimestamp(fileUploadTableObj.getSaasFileId(), fileUploadTableObj.getTimestamp())) {
                    try {
//                        log.info("FileUploadTable object saasFileId: {}", fileUploadTableObj.getSaasFileId());
//                        log.info("FileUploadTable object timestamp: {}", fileUploadTableObj.getTimestamp());
                        log.info("FileUploadTable Obj ID : {} " , fileUploadTableObj.getId());
                        fileUploadRepository.save(fileUploadTableObj);
                        log.info("scan start : {} in {}", file.getName(), filePath);
                        scanUtil.scanFile(filePath, fileUploadTableObj, file.getMimeType(), s3UploadPath);
                        if (fileUploadTableObj.getId() == null){
                            log.error("FileUploadTable id is null");
                            return null;
                        }
//                        messageSender.sendMessage(fileUploadTableObj.getId());
                    } catch (Exception e) {
                        log.error("Error saving file_upload table: {}", e.getMessage(), e);
                    }
                    log.info("File uploaded successfully in file_upload table: {}", file_name);
                } else {
                    log.warn("Duplicate file detected in FileUploadTable: {}", file_name);
                }
                if (activities.getSaasFileId() == null){
                    log.error("Saas file id is null");
                    return null;
                }
                if (activities.getEventTs() == null){
                    log.error("Event timestamp is null");
                    return null;
                }
                if (!activitiesRepository.existsBySaasFileIdAndEventTs(activities.getSaasFileId(), activities.getEventTs())) {
                    try {
                        activitiesRepository.save(activities);
                        if (activities.getId() == null){
                            log.error("Activities id is null");
                            return null;
                        }
                        log.info("Activity logged successfully activity table: {}", file_name);
                    } catch (Exception e) {
                        log.error("Error saving activities table: {}", e.getMessage(), e);
                    }
                    try {
                        messageSender.sendGroupingMessage(activities.getId());
                    } catch (Exception e) {
                        log.error("Error sending message to grouping_queue: {}", e.getMessage(), e);
                    }
                } else {
                    log.warn("Duplicate activity detected in Activities Table: {}", file_name);
                }
            } catch (Exception e) {
                log.error("Error while converting and saving entities: {}", e.getMessage(), e);
            }
        }
        if (file.getMimeType() == null || file.getFileExtension() == null){
            log.error("Mime type or file extension is null");
            return null;
        }
//        uploadFileToS3(filePath, s3UploadPath);
        return null;
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

    public void deleteFileInS3(String filePath) {
        try {
            // 삭제 요청 생성
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();

            // S3에서 파일 삭제
            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted successfully from S3: " + filePath);

        } catch (S3Exception e) {
            // 예외 처리
            log.info("Error deleting file from S3: " + e.awsErrorDetails().errorMessage());
        }
    }
}
