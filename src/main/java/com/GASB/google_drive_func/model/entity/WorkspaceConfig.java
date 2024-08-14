package com.GASB.google_drive_func.model.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "workspace_config")
public class WorkspaceConfig {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "workspace_name", nullable = false, length = 100)
    private String workspaceName;

    @Column(name = "saas_admin_email", nullable = false, length = 100)
    private String saasAdminEmail;

    @Column(name = "token", nullable = false, length = 100)
    private String token;

    @Column(name = "webhook")
    private String webhook;

    @Column(name = "alias")
    private String alias;

    @Column(name = "register_date", nullable = false)
    private Timestamp registerDate;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private OrgSaaS orgSaas;

    @Builder
    public WorkspaceConfig(String workspaceName, String saasAdminEmail, String token, String webhook, String alias, Timestamp registerDate, OrgSaaS orgSaas) {
        this.workspaceName = workspaceName;
        this.saasAdminEmail = saasAdminEmail;
        this.token = token;
        this.webhook = webhook;
        this.alias = alias;
        this.registerDate = registerDate;
        this.orgSaas = orgSaas;
    }

    // 복사본을 반환하도록 수정
    public Timestamp getRegisterDate() {
        return (Timestamp) registerDate.clone();
    }
}
