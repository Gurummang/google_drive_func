package com.GASB.google_drive_func.model.dto.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlackFileCountDto {
    private int totalFiles;
    private int sensitiveFiles;
    private int maliciousFiles;
    private int connectedAccounts;
}
