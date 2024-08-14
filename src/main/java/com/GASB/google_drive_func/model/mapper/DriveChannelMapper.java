package com.GASB.google_drive_func.model.mapper;

import com.GASB.google_drive_func.model.entity.ChannelList;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.google.api.services.drive.model.Drive;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DriveChannelMapper {

//    public List<ChannelList> toEntity(List<Conversation> conversations, OrgSaaS orgSaas) {
//        return conversations.stream().map(conversation -> toEntity(conversation, orgSaas)).collect(Collectors.toList());
//    }

    public ChannelList toEntity(OrgSaaS orgSaas, Drive drive) {
        return ChannelList.builder()
                .channelId(drive.getId())
                .channelName(drive.getName())
                .orgSaas(orgSaas)
                .build();
    }

}
