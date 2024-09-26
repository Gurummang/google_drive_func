package com.GASB.google_drive_func.service.GoogleUtil;

import com.GASB.google_drive_func.model.entity.GooglePageToken;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.repository.channel.GooglePageTokenRepo;
import com.GASB.google_drive_func.model.repository.org.OrgSaaSRepo;
import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Change;
import com.google.api.services.drive.model.Channel;
import com.google.api.services.drive.model.StartPageToken;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.weaver.ast.Or;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class GoogleDriveWebhookManager {
    private final WorkspaceConfigRepo workspaceConfigRepo;
    private final Map<Integer, String> channelIds = new ConcurrentHashMap<>();
    private final GoogleUtil googleUtil;
    private final GooglePageTokenRepo googlePageTokenRepo;
    private final OrgSaaSRepo orgSaaSRepo;

    public GoogleDriveWebhookManager(WorkspaceConfigRepo workspaceConfigRepo, GoogleUtil googleUtil,
                                     GooglePageTokenRepo googlePageTokenRepo, OrgSaaSRepo orgSaaSRepo) {
        this.workspaceConfigRepo = workspaceConfigRepo;
        this.googleUtil = googleUtil;
        this.googlePageTokenRepo = googlePageTokenRepo;
        this.orgSaaSRepo = orgSaaSRepo;
    }

    public CompletableFuture<Void> setWebhook(int workspaceId) {
        return CompletableFuture.runAsync(() -> {
            try {
                createOrUpdateSubscription(workspaceId);
            } catch (Exception e) {
                log.error("Error setting webhook for workspace {}: {}", workspaceId, e.getMessage(), e);
                throw new CompletionException(e);
            }
        });
    }

    private void createOrUpdateSubscription(int workspaceId) throws Exception {
        Drive drive = googleUtil.getDriveService(workspaceId);
        String webhookUrl = workspaceConfigRepo.getWebHookUrl(workspaceId)
                .orElseThrow(() -> new IllegalStateException("Webhook URL not configured for workspace " + workspaceId));

        String channelId = channelIds.computeIfAbsent(workspaceId, k -> UUID.randomUUID().toString());

        Channel channel = new Channel()
                .setId(channelId)
                .setType("web_hook")
                .setAddress(webhookUrl)
                .setExpiration(System.currentTimeMillis() +  7 * 24 * 60 * 60 * 1000L); // 1 hour from now





        // save page token to database
        // orgsaas오브젝트 연결 필요
        OrgSaaS orgSaaSObj = orgSaaSRepo.findById(workspaceId).orElseThrow(() -> new IllegalStateException("Workspace not found"));


        String pageToken = drive.changes().getStartPageToken()
                .setDriveId(orgSaaSObj.getSpaceId())
                .setSupportsAllDrives(true)
                .execute()
                .getStartPageToken();
        log.info("Fetched start page token: {}", pageToken);

        GooglePageToken googlePageToken = GooglePageToken.builder()
                .orgSaaS(orgSaaSObj)
                .channelId(channelId)
                .pageToken(pageToken)
                .build();
        if (googlePageToken == null){
            log.info("googlePageToken is null");
        }
        googlePageTokenRepo.save(Objects.requireNonNull(googlePageToken));

        try {
            drive.changes().watch(pageToken, channel)
                    .setDriveId(orgSaaSObj.getSpaceId())
                            .setIncludeRemoved(true)
                            .setIncludeItemsFromAllDrives(true)
                            .setIncludeCorpusRemovals(true)
                            .setSupportsAllDrives(true)
                            .execute();
            log.info("Started watching drive changes for workspace {} from pageToken: {}", workspaceId, pageToken);
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 409) {
                log.warn("Channel already exists for workspace {}. Stopping existing channel and creating a new one.", workspaceId);
                stopChannel(drive, channelId);
                drive.changes().watch(pageToken, channel)
                        .setIncludeRemoved(true)
                        .setIncludeItemsFromAllDrives(true)
                        .setSupportsAllDrives(true)
                        .execute();
            } else {
                throw e;
            }
        }
    }

    private void stopChannel(Drive drive, String channelId) throws IOException {
        Channel channelToStop = new Channel().setId(channelId);
        drive.channels().stop(channelToStop).execute();
    }

    // 주기적으로 실행되어야 하는 메서드
    @Scheduled(cron = "0 0 1 * * ?") // 매일 새벽 1시에 실행
    public void renewAllSubscriptions() {
        for (Integer workspaceId : channelIds.keySet()) {
            try {
                createOrUpdateSubscription(workspaceId);
            } catch (Exception e) {
                log.error("Failed to renew subscription for workspace {}: {}", workspaceId, e.getMessage(), e);
            }
        }
    }
}
