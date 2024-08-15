package com.GASB.google_drive_func.controller;

import com.GASB.google_drive_func.annotation.JWT.ValidateJWT;
import com.GASB.google_drive_func.model.dto.TopUserDTO;
import com.GASB.google_drive_func.model.dto.file.DriveFileCountDto;
import com.GASB.google_drive_func.model.dto.file.DirveFileSizeDto;
import com.GASB.google_drive_func.model.dto.file.DriveRecentFileDTO;
import com.GASB.google_drive_func.model.entity.Saas;
import com.GASB.google_drive_func.model.repository.org.AdminRepo;
import com.GASB.google_drive_func.model.repository.org.SaasRepo;
import com.GASB.google_drive_func.service.file.DriveFileService;
import com.GASB.google_drive_func.service.file.FileUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
@RequestMapping("/api/v1/board/google-drive")
public class DriveBoardController {
    private final FileUtil fileUtil;
    private final SaasRepo saasRepo;
    private final AdminRepo adminRepo;

    @Autowired
    public DriveBoardController(FileUtil fileUtil, SaasRepo saasRepo, AdminRepo adminRepo) {
        this.fileUtil = fileUtil;
        this.saasRepo = saasRepo;
        this.adminRepo = adminRepo;
    }

    @GetMapping("/files/size")
    @ValidateJWT
    public ResponseEntity<Map<String,?>> fetchFileSize(HttpServletRequest servletRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (servletRequest.getAttribute("error") != null) {
                String errorMessage = (String) servletRequest.getAttribute("error");
                response.put("status", 401);
                response.put("error_message", errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            String email = (String) servletRequest.getAttribute("email");
            int orgId = adminRepo.findByEmail(email).get().getOrg().getId();
            DirveFileSizeDto slackFileSizeDto = fileUtil.sumOfFileSize(orgId, 6);

            // 응답에 status 추가
            response.put("status", 200);
            response.put("data", slackFileSizeDto);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", 500);
            response.put("data", new DirveFileSizeDto(0, 0, 0));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    @GetMapping("/files/count")
    @ValidateJWT
    public ResponseEntity<Map<String, ?>> fetchFileCount(HttpServletRequest servletRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (servletRequest.getAttribute("error") != null) {
                String errorMessage = (String) servletRequest.getAttribute("error");
                response.put("status", 401);
                response.put("error_message", errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            String email = (String) servletRequest.getAttribute("email");
            int orgId = adminRepo.findByEmail(email).get().getOrg().getId();
            DriveFileCountDto slackFileCountDto = fileUtil.FileCountSum(orgId, 6);
            response.put("status", 200);
            response.put("data", slackFileCountDto);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", 500);
            response.put("data", new DriveFileCountDto(0, 0, 0, 0));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/files/recent")
    @ValidateJWT
    public ResponseEntity<Map<String, ?>> fetchRecentFiles(HttpServletRequest servletRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (servletRequest.getAttribute("error") != null) {
                String errorMessage = (String) servletRequest.getAttribute("error");
                response.put("status", 401);
                response.put("error_message", errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            String email = (String) servletRequest.getAttribute("email");
            int orgId = adminRepo.findByEmail(email).get().getOrg().getId();
            Saas saasObject = saasRepo.findById(6).orElse(null);
            List<DriveRecentFileDTO> recentFiles = fileUtil.DriveRecentFiles(orgId, Objects.requireNonNull(saasObject).getId().intValue());
            response.put("status", 200);
            response.put("data", recentFiles);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", 500);
            response.put("data", Collections.singletonList(new DriveRecentFileDTO("Error", "Server Error", "N/A", LocalDateTime.now())));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/user-ranking")
    @ValidateJWT
    public ResponseEntity<Map<String, ?>> fetchUserRanking(HttpServletRequest servletRequest) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (servletRequest.getAttribute("error") != null) {
                String errorMessage = (String) servletRequest.getAttribute("error");
                response.put("status", 401);
                response.put("error_message", errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            String email = (String) servletRequest.getAttribute("email");
            int orgId = adminRepo.findByEmail(email).get().getOrg().getId();
            Saas saasObject = saasRepo.findById(6).orElse(null);
            CompletableFuture<List<TopUserDTO>> future = fileUtil.getTopUsersAsync(orgId, Objects.requireNonNull(saasObject).getId().intValue());
            List<TopUserDTO> topuser = future.get();
            response.put("status", 200);
            response.put("data", topuser);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", 500);
            response.put("data", Collections.singletonList(new TopUserDTO("Error", 0L, 0L, LocalDateTime.now())));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
