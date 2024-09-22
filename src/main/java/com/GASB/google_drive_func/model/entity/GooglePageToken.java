package com.GASB.google_drive_func.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "google_page_token")
public class GooglePageToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @ManyToOne
    @JoinColumn(name = "org_saas_id", nullable = false, referencedColumnName = "id")
    private OrgSaaS orgSaaS;

    @Column(name = "channel_id", nullable = false)
    private String channelId;

    @Column(name = "page_token", nullable = false)
    private String pageToken;

    @Builder
    public GooglePageToken(OrgSaaS orgSaaS, String channelId, String pageToken) {
        this.orgSaaS = orgSaaS;
        this.channelId = channelId;
        this.pageToken = pageToken;
    }
}
