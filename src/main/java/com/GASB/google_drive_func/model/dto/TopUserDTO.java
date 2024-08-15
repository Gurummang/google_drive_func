package com.GASB.google_drive_func.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TopUserDTO {
    private String userName;
    private Long sensitiveFilesCount;
    private Long maliciousFilesCount;
    private LocalDateTime lastUploadedTimestamp;
}