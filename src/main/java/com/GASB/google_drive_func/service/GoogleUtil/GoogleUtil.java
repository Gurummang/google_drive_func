package com.GASB.google_drive_func.service.GoogleUtil;

import com.GASB.google_drive_func.model.entity.WorkspaceConfig;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.GASB.google_drive_func.model.repository.user.MonitoredUserRepo;
import com.GASB.google_drive_func.service.AESUtil;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class GoogleUtil {

    private static final String APPLICATION_NAME = "grummang-google-dirve-func";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final WorkspaceConfigRepo workspaceConfigRepo;

    @Autowired
    public GoogleUtil(WorkspaceConfigRepo workspaceConfigRepo) {
        this.workspaceConfigRepo = workspaceConfigRepo;
    }

    @Value("${aes.key}")
    private String key;

    public Credential selectToken(int workspace_id) {
        try {
            // workspace_id를 통해 해당 workspace의 token을 가져옴
            WorkspaceConfig workspaceConfig = workspaceConfigRepo.findById(workspace_id).orElse(null);
            if (workspaceConfig == null) { // 수정: 잘못된 workspace ID 처리
                throw new IllegalArgumentException("Invalid workspace ID");
            }
            return new Credential(BearerToken.authorizationHeaderAccessMethod())
                    .setAccessToken(AESUtil.decrypt(workspaceConfig.getToken(),key)); // 토큰을 반환
        } catch (Exception e) {
            log.error("An error occurred while selecting the token: {}", e.getMessage(), e);
            return null;
        }
    }

    public Drive getDriveService(int workspace_id) throws Exception {
        try {
            return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, selectToken(workspace_id))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            log.error("An error occurred while creating the Drive service: {}", e.getMessage(), e);
            throw e;
        }
    }


    // 수정: 토큰 갱신
//    public void refreshToken(int workspace_id) {
//        try {
//            WorkspaceConfig workspaceConfig = workspaceConfigRepo.findById(workspace_id).orElse(null);
//            if (workspaceConfig == null) { // 수정: 잘못된 workspace ID 처리
//                throw new IllegalArgumentException("Invalid workspace ID");
//            }
//            String refreshToken = AESUtil.decrypt(workspaceConfig.getRefreshToken(), key);
//            if (Objects.isNull(refreshToken)) {
//                throw new IllegalArgumentException("Invalid refresh token");
//            }
//            TokenResponse tokenResponse = new TokenResponse();
//            tokenResponse.setRefreshToken(refreshToken);
//            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
//                    GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, workspaceConfig.getClientId(), workspaceConfig.getClientSecret(),
//                    workspaceConfig.getScopes())
//                    .build();
//            Credential credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
//            tokenResponse = flow.newTokenRequest(credential.getRefreshToken()).setScopes(workspaceConfig.getScopes()).execute();
//            workspaceConfig.setToken(AESUtil.encrypt(tokenResponse.getAccessToken(), key));
//            workspaceConfigRepo.save(workspaceConfig);
//        } catch (Exception e) {
//            log.error("An error occurred while refreshing the token: {}", e.getMessage(), e);
//        }
//    }


    private void TokenUpdate(String Token, int workdpace_id){
        try {
            String freshToken = AESUtil.encrypt(Token, key);
            workspaceConfigRepo.updateToken(workdpace_id, freshToken);
        } catch (Exception e) {
            log.error("An error occurred while updating the token: {}", e.getMessage(), e);
        }
    }
}
