package com.GASB.google_drive_func.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "org_saas")
public class OrgSaaS {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;

    @ManyToOne
    @JoinColumn(name = "org_id", nullable = false)
    private Org org;

    @ManyToOne
    @JoinColumn(name = "saas_id", nullable = false)
    private Saas saas;

    @Column(name = "status", nullable = false)
    private int status;

    @Column(name = "space_id", unique = true)
    private String spaceId;

    @Column(name = "security_score")
    private int securityScore;

    @OneToMany(mappedBy = "orgSaas", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChannelList> channels;

    @OneToOne(mappedBy = "orgSaas", cascade = CascadeType.ALL)
    private WorkspaceConfig config;

    // 복사본을 반환하도록 수정
    public List<ChannelList> getChannels() {
        return Collections.unmodifiableList(channels);
    }
}
