package com.GASB.google_drive_func.service.GoogleUtil;

import com.GASB.google_drive_func.model.repository.org.WorkspaceConfigRepo;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;
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

    public GoogleDriveWebhookManager(WorkspaceConfigRepo workspaceConfigRepo, GoogleUtil googleUtil) {
        this.workspaceConfigRepo = workspaceConfigRepo;
        this.googleUtil = googleUtil;
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
                .setExpiration(System.currentTimeMillis() + 60 * 60 * 1000L); // 1 hour from now

        String pageToken = drive.changes().getStartPageToken().execute().getStartPageToken();

        try {
            drive.changes().watch(pageToken, channel)
                            .setIncludeRemoved(true)
                            .setIncludeItemsFromAllDrives(true)
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
