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
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

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

    private final DriveFileService driveFileService;

    @Autowired
    public DriveBoardController(FileUtil fileUtil, SaasRepo saasRepo, AdminRepo adminRepo, DriveFileService driveFileService) {
        this.fileUtil = fileUtil;
        this.saasRepo = saasRepo;
        this.adminRepo = adminRepo;
        this.driveFileService = driveFileService;
    }

    @GetMapping("/files/size")
    @ValidateJWT
    public ResponseEntity<?> fetchFileSize(HttpServletRequest servletRequest) {
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
            DirveFileSizeDto driveFileSizeDto = fileUtil.sumOfFileSize(orgId, 6);

            return ResponseEntity.ok(driveFileSizeDto);
        } catch (IllegalArgumentException e) {
            response.put("status", 500);
            response.put("data", new DirveFileSizeDto(0, 0, 0));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            response.put("status", 500);
            response.put("data", new DirveFileSizeDto(0, 0, 0));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    @GetMapping("/files/count")
    @ValidateJWT
    public ResponseEntity<?> fetchFileCount(HttpServletRequest servletRequest) {
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
            DriveFileCountDto driveFileCountDto = fileUtil.FileCountSum(orgId, 6);

            return ResponseEntity.ok(driveFileCountDto);
        } catch (IllegalArgumentException e) {
            log.error("IllegalArgumentException :: Error fetching file count: {}", e.getMessage());
            response.put("status", 500);
            response.put("message", "Error fetching file count");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("Etc Exception :: Error fetching file count: {}", e.getMessage());
            response.put("status", 500);
            response.put("message", "Error fetching file count");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/files/recent")
    @ValidateJWT
    public ResponseEntity<?> fetchRecentFiles(HttpServletRequest servletRequest) {
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

            return ResponseEntity.ok(recentFiles);
        } catch (IllegalArgumentException e) {
            response.put("status", 500);
            response.put("data", Collections.singletonList(new DriveRecentFileDTO("Error", "Error", "Error", LocalDateTime.now())));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            response.put("status", 500);
            response.put("data", Collections.singletonList(new DriveRecentFileDTO("Error", "Error", "Error", LocalDateTime.now())));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/user-ranking")
    @ValidateJWT
    public ResponseEntity<?> fetchUserRanking(HttpServletRequest servletRequest) {
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

            return ResponseEntity.ok(topuser);
        } catch (IllegalArgumentException e) {
            response.put("status", 500);
            response.put("data", Collections.singletonList(new TopUserDTO("Error", 0L, 0L, LocalDateTime.now())));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (NullPointerException e){
            log.error("Error fetching user ranking in user-ranking api: {}", e.getMessage());
            response.put("status", 500);
            response.put("data", Collections.singletonList(new TopUserDTO("Error", 0L, 0L, LocalDateTime.now())));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception e) {
            log.error("Error fetching user ranking in user-ranking api: {}", e.getMessage());
            response.put("status", 500);
            response.put("data", Collections.singletonList(new TopUserDTO("Error", 0L, 0L, LocalDateTime.now())));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/files/delete")
    @ValidateJWT
    public ResponseEntity<?> deleteFiles(HttpServletRequest servletRequest, @RequestBody List<Map<String, String>> requests) {
        try {
            // JWT 인증 오류 처리
            if (servletRequest.getAttribute("error") != null) {
                String errorMessage = (String) servletRequest.getAttribute("error");
                Map<String, String> errorResponse = new HashMap<>();
                log.error("Error fetching user ranking in user-ranking api: {}", errorMessage);
                errorResponse.put("status", "401");
                errorResponse.put("error_message", errorMessage);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
            }

            Map<String, String> response = new HashMap<>();
            boolean allSuccess = true;
            List<Map<String, String>> failedFiles = new ArrayList<>();

            // 요청받은 파일 목록 처리
            for (Map<String, String> request : requests) {
                try {
                    int fileUploadTableIdx = Integer.parseInt(request.get("id"));
                    String fileHash = request.get("file_hash");
                    String fileName = request.get("file_name");
                    String filePath = request.get("path");

                    // 파일 삭제 시도
                    if (!driveFileService.fileDelete(fileUploadTableIdx, fileHash,fileName ,filePath)) {
                        allSuccess = false;
                        log.error("Failed to delete file with id: {}, hash: {}", fileUploadTableIdx, fileHash);
                        Map<String, String> failedFile = new HashMap<>();
                        failedFile.put("id", String.valueOf(fileUploadTableIdx));
                        failedFile.put("file_hash", fileHash);
                        failedFiles.add(failedFile);
                    }
                } catch (Exception e) {
                    log.error("Error deleting file", e);
                    allSuccess = false;
                }
            }

            // 전체 성공 여부에 따른 응답
            if (allSuccess) {
                response.put("status", "200");
                response.put("message", "All files deleted successfully");
            } else {
                response.put("status", "404");
                response.put("message", "Some files failed to delete");
                response.put("failed_files", failedFiles.toString()); // 실패한 파일 정보 포함
            }

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.error("Error deleting files", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.singletonMap("message", "Internal server error"));
        }
    }



}
