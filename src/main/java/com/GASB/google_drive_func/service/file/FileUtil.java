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
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.nio.charset.StandardCharsets;
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

    private static final Path BASE_PATH = Paths.get("downloads");

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public String getFullPath(File file, String SaaSName, String orgName, String hash,String DriveName, Drive driveService) {
        List<String> pathParts = new ArrayList<>();
        String parentId = file.getParents() != null && !file.getParents().isEmpty() ? file.getParents().get(0) : null;

        while (parentId != null) {
            try {
                File parentFile = driveService.files().get(parentId)
                        .setFields("id, name, parents")
                        .setSupportsAllDrives(true)
                        .execute();
                log.info("Parent file: {}", parentFile);
                pathParts.add(0, parentFile.getName());
                parentId = parentFile.getParents() != null && !parentFile.getParents().isEmpty() ? parentFile.getParents().get(0) : null;
            } catch (IOException e) {
                log.error("Failed to get parent file", e);
                break;
            }
        }

        // 해시값을 경로에 추가
        pathParts.add(hash);

        // 파일 이름을 경로에 추가
        pathParts.add(file.getName());

        // "Drive"를 DriveName으로 변경
        pathParts.set(pathParts.indexOf("Drive"), DriveName);
        // 드라이브 이름을 맨 앞에 추가
        pathParts.add(0, SaaSName);

        pathParts.add(0, orgName);

        return String.join("/", pathParts);
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
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hash = digest.digest(fileData);
        return bytesToHex(hash);
    }


    //TLSH 해시 계산
    private Tlsh computeTlsHash(byte[] fileData) throws IOException {
        if (fileData == null) {
            throw new IllegalArgumentException("fileData cannot be null");
        }
        final int BUFFER_SIZE = 4096;
        TlshCreator tlshCreator = new TlshCreator();

        try (InputStream is = new ByteArrayInputStream(fileData)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buf)) != -1) {
                tlshCreator.update(buf, 0, bytesRead);
            }
        } catch (IOException e) {
            throw new IOException("Error computing TLSH hash", e);
        }

        return tlshCreator.getHash();
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
            com.google.api.services.drive.model.File file = service.files().get(fileId)
                    .setSupportsAllDrives(true)
                    .setFields("id, name, size, mimeType")
                    .execute();

            log.info("Downloading file: {} (ID: {})", file.getName(), file.getId());
            log.info("File size: {} bytes, MIME type: {}", file.getSize(), file.getMimeType());

            Path parentDir = Paths.get(filePath).getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // 바이트 배열로 데이터를 읽어들입니다.
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            service.files().get(fileId).executeMediaAndDownloadTo(outputStream);

            // 파일을 저장합니다.
            try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
                outputStream.writeTo(fileOutputStream);
            }

            byte[] fileData = outputStream.toByteArray();
            long downloadedSize = fileData.length;

            if (downloadedSize == file.getSize()) {
                log.info("Download verified: File size matches ({} bytes)", downloadedSize);
            } else {
                log.warn("Download size mismatch: Expected {} bytes, got {} bytes", file.getSize(), downloadedSize);
            }

            return fileData; // 바이트 배열 반환

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
        String savedPath = getFullPath(file, saasname, OrgName, hash, workspaceName, service);
        String filePath = BASE_PATH.resolve(file.getName()).toString();


        MonitoredUsers user = monitoredUserRepo.fineByUserId(file.getLastModifyingUser().getPermissionId(),workspaceId).orElse(null);
        StoredFile storedFileObj = driveFileMapper.toStoredFileEntity(file, hash, savedPath);
        FileUploadTable fileUploadTableObj = driveFileMapper.toFileUploadEntity(file, orgSaaSObject, hash);
        Activities activities = driveFileMapper.toActivityEntity(file, event_type, user, savedPath, tlsh);
        synchronized (this) {
            try {

                if (!storedFilesRepository.existsBySaltedHash(storedFileObj.getSaltedHash())) {
                    storedFilesRepository.save(storedFileObj);
                    try {
                        messageSender.sendMessage(storedFileObj.getId());
                    } catch (Exception e) {
                        log.error("Error sending message to file_queue: {}", e.getMessage(), e);
                    }
                    log.info("File uploaded successfully stored_file table: {}", file.getName());
                } else {
                    log.warn("Duplicate file detected in StoredFile: {}", file.getName());
                }


                if (!fileUploadRepository.existsBySaasFileIdAndTimestamp(fileUploadTableObj.getSaasFileId(), fileUploadTableObj.getTimestamp())) {
                    try {
                        fileUploadRepository.save(fileUploadTableObj);
                    } catch (Exception e) {
                        log.error("Error saving file_upload table: {}", e.getMessage(), e);
                    }
                    log.info("File uploaded successfully in file_upload table: {}", file.getName());
                } else {
                    log.warn("Duplicate file detected in FileUploadTable: {}", file.getName());
                }
                if (!activitiesRepository.existsBySaasFileIdAndEventTs(activities.getSaasFileId(), activities.getEventTs())) {
                    try {
                        activitiesRepository.save(activities);
                        log.info("Activity logged successfully activity table: {}", file.getName());
                    } catch (Exception e) {
                        log.error("Error saving activities table: {}", e.getMessage(), e);
                    }
                    try {
                        messageSender.sendGroupingMessage(activities.getId());
                    } catch (Exception e) {
                        log.error("Error sending message to grouping_queue: {}", e.getMessage(), e);
                    }
                } else {
                    log.warn("Duplicate activity detected in Activities Table: {}", file.getName());
                }
            } catch (Exception e) {
                log.error("Error while converting and saving entities: {}", e.getMessage(), e);
            }
        }
        scanUtil.scanFile(filePath, fileUploadTableObj, file.getMimeType(), file.getFileExtension());
        uploadFileToS3(filePath, savedPath);
        return null;
    }

    private void uploadFileToS3(String filePath, String s3Key) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.putObject(putObjectRequest, Paths.get(filePath));
            log.info("File uploaded successfully to S3: {}", s3Key);
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage(), e);
        }
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
