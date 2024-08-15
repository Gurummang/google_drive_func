package com.GASB.google_drive_func.service.GoogleUtil;

import com.GASB.google_drive_func.model.entity.WorkspaceConfig;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Objects;

@Service
@Slf4j
public class GoogleUtil {

    private static final String APPLICATION_NAME = "grummang-google-dirve-func";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    private final GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow;
    private final WorkspaceConfigRepo workspaceConfigRepo;

    @Autowired
    public GoogleUtil(GoogleAuthorizationCodeFlow googleAuthorizationCodeFlow, WorkspaceConfigRepo workspaceConfigRepo) {
        this.googleAuthorizationCodeFlow = googleAuthorizationCodeFlow;
        this.workspaceConfigRepo = workspaceConfigRepo;
    }

    private Credential selectToken(int workspace_id) {
        try {
            // workspace_id를 통해 해당 workspace의 token을 가져옴
            WorkspaceConfig workspaceConfig = workspaceConfigRepo.findById(workspace_id).orElse(null);
            if (workspaceConfig == null) { // 수정: 잘못된 workspace ID 처리
                throw new IllegalArgumentException("Invalid workspace ID");
            }
            return new Credential(BearerToken.authorizationHeaderAccessMethod())
                    .setAccessToken(workspaceConfig.getToken()); // 토큰을 반환
        } catch (Exception e) {
            log.error("An error occurred while selecting the token: {}", e.getMessage(), e);
            return null;
        }
    }

    private TokenResponse refreshToken(GoogleAuthorizationCodeFlow flow, String refreshToken) throws IOException {
        return flow.newTokenRequest(refreshToken).setGrantType("refresh_token").execute();
    }

    private Credential refreshAccessToken(Credential credential, int workspace_id) throws IOException {
        TokenResponse response = refreshToken(googleAuthorizationCodeFlow, credential.getRefreshToken());
        log.info("Access token refreshed successfully.");
        credential.setAccessToken(response.getAccessToken());
        credential.setExpiresInSeconds(response.getExpiresInSeconds());
        // 수정: 새로운 토큰을 DB에 저장
        workspaceConfigRepo.updateToken(workspace_id, response.getAccessToken());
        return credential;
    }

    protected Credential getCredentials() throws Exception {

        // 기존의 자격 증명을 무시하고 항상 새로 발급받도록 함
        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8088).setCallbackPath("/login/oauth2/code/google").build();


        // 새로 발급된 자격 증명을 DB에 저장
//        workspaceConfigRepo.updateToken(workspace_id, credential.getAccessToken());

        return new AuthorizationCodeInstalledApp(googleAuthorizationCodeFlow, receiver).authorize("user");
    }


    public Drive getDriveService(int workspace_id) throws Exception {
        try {
            String accessToken = getCredentials().getAccessToken();
//            log.info("Access token: {}", accessToken);

            Credential credential = selectToken(workspace_id);

            return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        } catch (Exception e) {
            log.error("An error occurred while creating the Drive service: {}", e.getMessage(), e);
            throw e;
        }
    }

    public void getTokenExpiredTime(Credential credential) {
        log.info("Token Expired Time: {}", credential.getExpiresInSeconds());
    }
}
