package com.GASB.google_drive_func.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "channel_list")
public class ChannelList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "org_saas_id", nullable = false, referencedColumnName = "id")
    private OrgSaaS orgSaas;

    @Column(name = "channel_id", nullable = false, unique = true, length = 100)
    private String channelId;

    @Column(name="channel_name", nullable = false, length = 100)
    private String channelName;

    @Builder
    public ChannelList(OrgSaaS orgSaas, String channelId, String channelName) {
        this.orgSaas = orgSaas;
        this.channelId = channelId;
        this.channelName = channelName;
    }
}
