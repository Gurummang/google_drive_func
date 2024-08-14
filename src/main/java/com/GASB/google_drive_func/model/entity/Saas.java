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
@Table(name = "saas")
public class Saas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "saas_name", nullable = false, length = 100)
    private String saasName;

    @OneToMany(mappedBy = "saas", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrgSaaS> orgSaaSList;

    @Builder
    public Saas(String saasName, List<OrgSaaS> orgSaaSList) {
        this.saasName = saasName;
        this.orgSaaSList = orgSaaSList;
    }

    // 복사본을 반환하도록 수정
    public List<OrgSaaS> getOrgSaaSList() {
        return Collections.unmodifiableList(orgSaaSList);
    }
}
