package com.GASB.google_drive_func.service.file;

import com.GASB.google_drive_func.model.entity.*;
import com.GASB.google_drive_func.model.mapper.DriveFileMapper;
import com.GASB.google_drive_func.model.repository.MonitoredUserRepo;
import com.GASB.google_drive_func.model.repository.files.FileActivityRepo;
import com.GASB.google_drive_func.model.repository.files.FileUploadRepository;
import com.GASB.google_drive_func.model.repository.files.StoredFileRepository;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static com.nimbusds.openid.connect.sdk.id.HashBasedPairwiseSubjectCodec.HASH_ALGORITHM;


@Slf4j
@Service
@RequiredArgsConstructor
public class FileUtil {

    private final WorkspaceConfigRepo worekSpaceRepo;
    private final StoredFileRepository storedFilesRepository;
    private final DriveFileMapper driveFileMapper;
    private final MonitoredUserRepo MonitoredUsersRepo;
    private final FileUploadRepository fileUploadRepository;
    private final FileActivityRepo activitiesRepository;
    private final S3Client s3Client;

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
        WorkspaceConfig config = worekSpaceRepo.findById(workspaceId).orElse(null);
        String workspaceName = Objects.requireNonNull(config).getWorkspaceName();

        String saasname = orgSaaSObject.getSaas().getSaasName();
        String OrgName = orgSaaSObject.getOrg().getOrgName();
        String savedPath = getFullPath(file, saasname, OrgName, hash, workspaceName, service);

        MonitoredUsers user = MonitoredUsersRepo.fineByUserId(file.getLastModifyingUser().getPermissionId()).orElse(null);
        synchronized (this) {
            try {
                StoredFile storedFileObj = driveFileMapper.toStoredFileEntity(file, hash, savedPath);
                if (!storedFilesRepository.existsBySaltedHash(storedFileObj.getSaltedHash())) {
                    storedFilesRepository.save(storedFileObj);
                    log.info("File uploaded successfully: {}", file.getName());
                } else {
                    log.warn("Duplicate file detected in StoredFile: {}", file.getName());
                }

                FileUploadTable fileUploadTableObj = driveFileMapper.toFileUploadEntity(file, orgSaaSObject, hash);
                if (!fileUploadRepository.existsBySaasFileIdAndTimestamp(fileUploadTableObj.getSaasFileId(), fileUploadTableObj.getTimestamp())) {
                    fileUploadRepository.save(fileUploadTableObj);
                    log.info("File uploaded successfully: {}", file.getName());
                } else {
                    log.warn("Duplicate file detected in FileUploadTable: {}", file.getName());
                }

                Activities activities = driveFileMapper.toActivityEntity(file, event_type, user, savedPath);
                if (!activitiesRepository.existsBySaasFileIdAndEventTs(activities.getSaasFileId(), activities.getEventTs())) {
                    activitiesRepository.save(activities);
                    log.info("Activity logged successfully: {}", file.getName());
                } else {
                    log.warn("Duplicate activity detected in Activities Table: {}", file.getName());
                }
            } catch (Exception e) {
                log.error("Error while converting and saving entities: {}", e.getMessage(), e);
            }
        }
        uploadFileToS3(BASE_PATH.resolve(file.getName()).toString(), savedPath);
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
}
