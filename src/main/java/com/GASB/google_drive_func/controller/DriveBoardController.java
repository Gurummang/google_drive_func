package com.GASB.google_drive_func.controller;

import com.GASB.google_drive_func.controller.RequestBody.DriveBody;
import com.GASB.google_drive_func.model.dto.TopUserDTO;
import com.GASB.google_drive_func.model.dto.file.DriveFileCountDto;
import com.GASB.google_drive_func.model.dto.file.DirveFileSizeDto;
import com.GASB.google_drive_func.model.dto.file.DriveRecentFileDTO;
import com.GASB.google_drive_func.model.entity.Saas;
import com.GASB.google_drive_func.model.repository.org.AdminRepo;
import com.GASB.google_drive_func.model.repository.org.SaasRepo;
import com.GASB.google_drive_func.service.file.DriveFileSerivce;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
@RequestMapping("/api/v1/board/google-drive")
public class DriveBoardController {
    private final DriveFileSerivce driveFileSerivce;
    private final SaasRepo saasRepo;
    private final AdminRepo adminRepo;

    @Autowired
    public DriveBoardController(DriveFileSerivce driveFileSerivce, SaasRepo saasRepo, AdminRepo adminRepo) {
        this.driveFileSerivce = driveFileSerivce;
        this.saasRepo = saasRepo;
        this.adminRepo = adminRepo;
    }

    @PostMapping("/files/size")
    public ResponseEntity<DirveFileSizeDto> fetchFileSize(@RequestBody DriveBody requestBody){
        try{
//            String email = (String) servletRequest.getAttribute("email");
            String email = requestBody.getEmail();
            int orgId = adminRepo.findByEmail(email).get().getOrg().getId();
            DirveFileSizeDto slackFileSizeDto = driveFileSerivce.sumOfFileSize(orgId,6);
            return ResponseEntity.ok(slackFileSizeDto);
        } catch (Exception e) {
            // log.error("Error fetching file size", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DirveFileSizeDto(0,0,0));
        }
    }

    @PostMapping("/files/count")
    public ResponseEntity<DriveFileCountDto> fetchFileCount(@RequestBody DriveBody requestBody){
        try{
//            String email = (String) servletRequest.getAttribute("email");
            // log.info("httpServletRequest: {}", servletRequest);
            String email = requestBody.getEmail();
            int orgId = adminRepo.findByEmail(email).get().getOrg().getId();
            DriveFileCountDto slackFileCountDto = driveFileSerivce.FileCountSum(orgId,6);
            return ResponseEntity.ok(slackFileCountDto);
        } catch (Exception e) {
            // log.error("Error fetching file count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DriveFileCountDto(0,0,0,0));
        }
    }
    @PostMapping("/files/recent")
    public ResponseEntity<List<DriveRecentFileDTO>> fetchRecentFiles(@RequestBody DriveBody requestBody) {
        try {
//            String email = (String) servletRequest.getAttribute("email");
            String email = requestBody.getEmail();
            int orgId = adminRepo.findByEmail(email).get().getOrg().getId();
            Saas saasObject = saasRepo.findById(6).orElse(null);
            List<DriveRecentFileDTO> recentFiles = driveFileSerivce.DriveRecentFiles(orgId, Objects.requireNonNull(saasObject).getId().intValue());
            return ResponseEntity.ok(recentFiles);
        } catch (Exception e) {
            // log.error("Error fetching recent files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonList(new DriveRecentFileDTO("Error", "Server Error", "N/A", LocalDateTime.now())));
        }
    }

    @PostMapping("/user-ranking")
    public ResponseEntity<List<TopUserDTO>> fetchUserRanking(@RequestBody DriveBody requestBody) {
        try {
//            String email = (String) servletRequest.getAttribute("email");
            String email = requestBody.getEmail();
            int orgId = adminRepo.findByEmail(email).get().getOrg().getId();
//            Saas saasObject = saasRepo.findBySaasName("GoogleDrive").orElse(null);
            Saas saasObject = saasRepo.findById(6).orElse(null);
            CompletableFuture<List<TopUserDTO>> future = driveFileSerivce.getTopUsersAsync(orgId, Objects.requireNonNull(saasObject).getId().intValue());
            List<TopUserDTO> topuser = future.get();

            return ResponseEntity.ok(topuser);
        } catch (RuntimeException e){
            // log.error("Error fetching recent files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonList(new TopUserDTO("Error", 0L, 0L, LocalDateTime.now())));
        } catch (Exception e) {
            // log.error("Error fetching recent files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonList(new TopUserDTO("Error", 0L, 0L, LocalDateTime.now())));
        }
    }
}
