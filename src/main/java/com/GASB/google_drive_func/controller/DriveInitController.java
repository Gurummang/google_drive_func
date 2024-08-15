//package com.GASB.google_drive_func.controller;
//
//import com.GASB.google_drive_func.model.entity.Saas;
//import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
//import com.GASB.google_drive_func.model.repository.org.SaasRepo;
//import com.GASB.google_drive_func.service.DriveApiService;
//import com.GASB.google_drive_func.service.GoogleUtil.GoogleUtil;
//import com.GASB.google_drive_func.service.file.DriveFileService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.CompletableFuture;
//
//@RestController
//@Slf4j
//@RequestMapping("/api/v1/connect/google-drive")
//public class DriveInitController {
//
//    private final DriveApiService driveApiService;
//    private final GoogleUtil googleUtil;
//    private final OrgSaaSRepo orgSaaSRepo;
//    private final SaasRepo SaasRepo;
//    private final DriveFileService driveFileService;
//
//    private static final List<String> spaceIdList = List.of("0ABuaF1uQELhqUk9PVA", "0ABfguJfTe9jEUk9PVA");
//
//    @Autowired
//    public DriveInitController(DriveApiService driveApiService, GoogleUtil googleUtil
//            , OrgSaaSRepo orgSaaSRepo, SaasRepo SaasRepo
//            , DriveFileService driveFileService) {
//        this.driveApiService = driveApiService;
//        this.orgSaaSRepo = orgSaaSRepo;
//        this.SaasRepo = SaasRepo;
//        this.driveFileService = driveFileService;
//        this.googleUtil = googleUtil;
//    }
//
//    @GetMapping("/files")
//    public ResponseEntity<Map<String, String>> fetchAndSaveFiles() {
//        Map<String, String> response = new HashMap<>();
//        try {
//            int org_id = 1;
//            Saas google_drive_saas_obj = SaasRepo.findById(6).orElse(null);
//
//            List<CompletableFuture<Void>> futures = new ArrayList<>();
//            futures.add(driveFileService.fetchAndStoreFiles(1, "file_upload"));
//
//            // 모든 비동기 작업이 완료될 때까지 기다림
//            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//
//            response.put("status", "success");
//            response.put("message", "Files saved successfully");
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            response.put("status", "error");
//            response.put("message", "Error fetching files : " + e.getMessage());
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }
//
//
//
//
//    @GetMapping("/users")
//    public ResponseEntity<Map<String,String>> fetchAndSaveUsers(/*@RequestBody int workspace_id*/) throws Exception {
//        Map<String,String> response = new HashMap<>();
//        try {
//            int org_id = 1;
//            //이렇게 함으로써 구글드라이브에 가져올 수 있는 모든 드라이브에 접근 가능한 user목록을 얻어온다.
//            Saas saasObj = SaasRepo.findById(6).orElse(null);
//            for (String spaceId : spaceIdList ){
//                driveApiService.fetchUserList(16,spaceId);
//            }
//            response.put("status","success");
//            response.put("message", "User Fetching Successfully");
//            return ResponseEntity.ok(response);
//        } catch (Exception e) {
//            response.put("status", "error");
//            response.put("message", "Error fetching files");
//            log.error("User fetching failed : " + e);
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
//        }
//    }
//
////    @GetMapping("/all")
////    public ResponseEntity<Map<String, String>> fetchAndSaveAll(@RequestBody String email) throws Exception {
////        Map<String, String> response = new HashMap<>();
////        try{
////            int org_id = 1;
////            Saas google_drive_saas_obj = SaasRepo.findById(3).orElse(null);
////            List<String> google_dribe_saas_list = orgSaaSRepo.findSpaceIdByOrgIdAndSaas(org_id, google_drive_saas_obj);
////            for (String sapceId : google_dribe_saas_list){
////                driveApiService.fetchUserList(16,sapceId);
////            }
////            for (String spaceId : google_dribe_saas_list){
////                driveFileService.fetchAndStoreFiles(1, "file_upload");
////            }
////            response.put("status", "success");
////            response.put("message", "All saved successfully");
////            return ResponseEntity.ok(response);
////        } catch (Exception e) {
////            response.put("status", "error");
////            response.put("message", "Error fetching files");
////            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
////        }
////    }
//}
//
//
