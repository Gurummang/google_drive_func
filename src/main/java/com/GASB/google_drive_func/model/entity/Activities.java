package com.GASB.google_drive_func.model.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "activities")
public class Activities {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private MonitoredUsers user;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Column(name = "saas_file_id", length = 64)
    private String saasFileId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "event_ts")
    private LocalDateTime eventTs;

    @Column(name = "upload_channel", length = 100)
    private String uploadChannel;

    @Builder
    public Activities(MonitoredUsers user, String eventType, String saasFileId, String fileName, LocalDateTime eventTs, String uploadChannel) {
        this.user = user;
        this.eventType = eventType;
        this.saasFileId = saasFileId;
        this.fileName = fileName;
        this.eventTs = eventTs;
        this.uploadChannel = uploadChannel;
    }
}
