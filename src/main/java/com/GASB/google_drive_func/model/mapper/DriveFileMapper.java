package com.GASB.google_drive_func.model.mapper;

import com.GASB.google_drive_func.model.entity.*;
import com.GASB.google_drive_func.model.repository.files.FileActivityRepo;
import com.GASB.google_drive_func.model.repository.files.FileUploadRepository;
import com.GASB.google_drive_func.model.repository.files.StoredFileRepository;
import com.google.api.services.drive.model.File;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
@Component
@Slf4j
@RequiredArgsConstructor
public class DriveFileMapper {


    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final FileActivityRepo fileActivityRepo;
    private final StoredFileRepository storedFileRepository;
    private final FileUploadRepository fileUploadRepository;

    public StoredFile toStoredFileEntity(File file, String hash, String filePath) {
        if (file == null) {
            return null;
        }
        return StoredFile.builder()
                .type(file.getFileExtension())
                .size(file.getSize().intValue())
                .savePath(bucketName + "/" + filePath)
                .saltedHash(hash)
                .build();
    }

    public FileUploadTable toFileUploadEntity(File file, OrgSaaS orgSaas, String hash) {
        if (file == null) {
            return null;
        }
        return FileUploadTable.builder()
                .orgSaaS(orgSaas)
                .saasFileId(file.getId())
                .hash(hash)
                .timestamp(LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(file.getCreatedTime().getValue()),
                                ZoneId.systemDefault()))
                .build();
    }

    public Activities toActivityEntity(File file, String eventType, MonitoredUsers user, String channel) {
        if (file == null) {
            return null;
        }
        return Activities.builder()
                .user(user)
                .eventType(eventType.isEmpty() ? "file_upload" : eventType)
                .saasFileId(file.getId())
                .fileName(file.getName())
                .eventTs(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(file.getCreatedTime().getValue()),
                        ZoneId.systemDefault()))
                .uploadChannel(file.getParents().isEmpty() ? null : channel)
                .build();
    }

    public void EntireEntityConverter(File file, String event_type, MonitoredUsers user, String channel, String hash, String filePath, OrgSaaS orgSaas) {
        try {

            StoredFile storedFile = toStoredFileEntity(file, hash, filePath);
            storedFileRepository.save(storedFile);

            FileUploadTable fileUploadTable = toFileUploadEntity(file, orgSaas, hash);
            fileUploadRepository.save(fileUploadTable);

            Activities activities = toActivityEntity(file, event_type, user, channel);
            fileActivityRepo.save(activities);

        } catch (Exception e) {
            log.error("Error while converting and saving entities: {}", e.getMessage(), e);
        }
    }



}
