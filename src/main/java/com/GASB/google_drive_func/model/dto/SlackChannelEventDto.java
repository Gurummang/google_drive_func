package com.GASB.google_drive_func.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlackChannelEventDto {
    private String from;
    private String event;
    private String saas;
    private String channelId;
    private String channelName;
}
