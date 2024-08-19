package com.GASB.google_drive_func.service;

import com.GASB.google_drive_func.model.entity.MonitoredUsers;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.mapper.DriveFileMapper;
import com.GASB.google_drive_func.model.mapper.DriveUserMapper;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class DriveApiService {

    private final GoogleUtil googleUtil;
    private final OrgSaaSRepo OrgSaaSRepo;
    private final MonitoredUserRepo monitoredUserRepo;
    private final DriveUserMapper DriveUserMapper;
    @Autowired
    public DriveApiService(GoogleUtil googleUtil, OrgSaaSRepo OrgSaaSRepo
            , MonitoredUserRepo MonitoredUsersRepo, DriveUserMapper DriveUserMapper) {
        this.googleUtil = googleUtil;
        this.OrgSaaSRepo = OrgSaaSRepo;
        this.monitoredUserRepo = MonitoredUsersRepo;
        this.DriveUserMapper = DriveUserMapper;
    }


    public FileList fetchFiles(Drive service, String DriveId){
        try {
            FileList result = service.files().list()
                    .setDriveId(DriveId)
                    .setIncludeItemsFromAllDrives(true)
                    .setSupportsAllDrives(true)
                    .setCorpora("drive")
                    .setQ("trashed = false")
                    .setFields("files")
                    .execute();
            return result;
        } catch (Exception e) {
            log.error("An error occurred while listing files: {}", e.getMessage(), e);
            return null;
        }
    }

    public PermissionList fetchUser(Drive service, String DriveId) {
        try {
            return service.permissions().list(DriveId)
                    .setSupportsAllDrives(true)
                    .setUseDomainAdminAccess(true)
                    .setFields("permissions(id,emailAddress,displayName,role)")
                    .execute();
        } catch (Exception e){
            log.error("An error occurred while listing users: {}", e.getMessage(), e);
            return null;
        }
    }
}
