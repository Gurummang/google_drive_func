package com.GASB.google_drive_func.model.mapper;

import com.GASB.google_drive_func.model.entity.MonitoredUsers;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.google.api.services.drive.model.Permission;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;

@Component
@RequiredArgsConstructor
public class DriveUserMapper {
    public MonitoredUsers toEntity(Permission permission, OrgSaaS orgSaaS) {
        return MonitoredUsers.builder()
                .userId(permission.getId())
                .orgSaaS(orgSaaS)
                .email(permission.getEmailAddress())
                .userName(permission.getDisplayName())
                .timestamp(new Timestamp(System.currentTimeMillis()))
                .build();
    }
}
