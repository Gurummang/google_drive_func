package com.GASB.google_drive_func.model.mapper;

import com.GASB.google_drive_func.model.entity.*;
import com.GASB.google_drive_func.model.repository.files.FileActivityRepo;
import com.GASB.google_drive_func.model.repository.files.FileUploadRepository;
import com.GASB.google_drive_func.model.repository.files.StoredFileRepository;
import com.google.api.client.util.DateTime;
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

    private final ZoneId zoneId = ZoneId.of("Asia/Seoul");

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

        LocalDateTime kstTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(file.getCreatedTime().getValue()), zoneId
        );

        return FileUploadTable.builder()
                .orgSaaS(orgSaas)
                .saasFileId(file.getId())
                .hash(hash)
                .deleted(false)
                .timestamp(kstTime)
                .build();
    }


    public Activities toActivityEntity(File file, String eventType, MonitoredUsers user, String channel, String tlsh) {
        if (file == null) {
            return null;
        }

        // 생성 시간의 null 체크
        DateTime createdTime = file.getCreatedTime();
        LocalDateTime eventTs = null;
        if (createdTime != null) {
            eventTs = LocalDateTime.ofInstant(Instant.ofEpochMilli(file.getCreatedTime().getValue()), zoneId);
        } else {
            log.warn("File created time is null: {}", file.getId());
        }

        // eventType null 체크
        if (eventType == null || eventType.isEmpty()) {
            eventType = "file_upload";
        }

        // file.getParents() null 체크
        String uploadChannel = null;
        if (file.getParents() != null && !file.getParents().isEmpty()) {
            uploadChannel = channel;
        }

        return Activities.builder()
                .user(user)
                .eventType(eventType)
                .saasFileId(file.getId())
                .fileName(file.getName())
                .eventTs(eventTs)  // eventTs가 null일 수 있음에 유의
                .uploadChannel(uploadChannel)
                .tlsh(tlsh)
                .build();
    }

}
