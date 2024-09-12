package com.GASB.google_drive_func.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "dlp_report")
public class DlpReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "file_id", nullable = false, referencedColumnName = "id")
    private StoredFile storedFile;

    @ManyToOne
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne
    @JoinColumn(name = "pii_id", nullable = false)
    private Pii pii;

    @Column(name = "info_cnt")
    private int infoCnt;

}
