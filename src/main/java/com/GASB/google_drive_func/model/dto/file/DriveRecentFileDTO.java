package com.GASB.google_drive_func.model.dto.file;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DriveRecentFileDTO {
    private String fileName;
    private String uploadedBy;
    private String fileType;
    private LocalDateTime uploadTimestamp;

    public DriveRecentFileDTO(String fileName, String uploadedBy, String fileType, LocalDateTime uploadTimestamp) {
        this.fileName = fileName;
        this.uploadedBy = uploadedBy;
        this.fileType = fileType;
        this.uploadTimestamp = uploadTimestamp;
    }
}
