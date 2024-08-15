package com.GASB.google_drive_func.service;

import com.GASB.google_drive_func.model.entity.MonitoredUsers;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.mapper.DriveUserMapper;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class DriveUserService {

    private final GoogleUtil googleUtil;
    private final OrgSaaSRepo orgSaaSRepo;
    private final DriveApiService driveApiService;
    private final DriveUserMapper driveUserMapper;
    private final MonitoredUserRepo monitoredUserRepo;

    @Autowired
    public DriveUserService(GoogleUtil googleUtil, OrgSaaSRepo orgSaaSRepo,
                            DriveApiService driveApiService, DriveUserMapper driveUserMapper,
                            MonitoredUserRepo monitoredUserRepo) {
        this.googleUtil = googleUtil;
        this.orgSaaSRepo = orgSaaSRepo;
        this.driveApiService = driveApiService;
        this.driveUserMapper = driveUserMapper;
        this.monitoredUserRepo = monitoredUserRepo;
    }

    @Transactional
    public CompletableFuture<Void> fetchUser(int workspaceId) {
        return CompletableFuture.runAsync(() -> {
            try {
                Drive service = googleUtil.getDriveService(workspaceId);
                OrgSaaS orgSaaSObject = orgSaaSRepo.findById(workspaceId).orElse(null);

                String spaceId = orgSaaSRepo.getSpaceID(workspaceId);
                PermissionList permissionList = driveApiService.fetchUser(service, spaceId);

                if (permissionList != null && !permissionList.isEmpty()) {
                    for (Permission permission : permissionList.getPermissions()) {
                        if (permission.getEmailAddress() != null) {
                            MonitoredUsers user = driveUserMapper.toEntity(permission, orgSaaSObject);
                            if (!monitoredUserRepo.existsByUserId(permission.getId())) {
                                monitoredUserRepo.save(user);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("An error occurred while fetching users: {}", e.getMessage(), e);
            }
        });
    }
}
