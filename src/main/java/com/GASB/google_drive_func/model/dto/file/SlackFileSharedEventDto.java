package com.GASB.google_drive_func.model.dto.file;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlackFileSharedEventDto {
    private String from;
    private String event;
    private String saas;
    private String fileId;
}
