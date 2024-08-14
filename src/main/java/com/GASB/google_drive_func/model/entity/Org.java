package com.GASB.google_drive_func.model.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "org")
public class Org {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "org_name", nullable = false, length = 100)
    private String orgName;

    @OneToMany(mappedBy = "org", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrgSaaS> orgSaaSList;

    @Builder
    public Org(String orgName, List<OrgSaaS> orgSaaSList) {
        this.orgName = orgName;
        this.orgSaaSList = orgSaaSList;
    }

    // 복사본을 반환하도록 수정
    public List<OrgSaaS> getOrgSaaSList() {
        return Collections.unmodifiableList(orgSaaSList);
    }
}
