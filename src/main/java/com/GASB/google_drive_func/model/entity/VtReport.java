package com.GASB.google_drive_func.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="vt_report")
public class VtReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "file_id", referencedColumnName = "id")
    private StoredFile storedFile;

    @Column(name = "type")
    private String type;

    @Column(name = "v3")
    private String v3;

    @Column(name = "alyac")
    private String aLYac;

    @Column(name = "kaspersky")
    private String kaspersky;

    @Column(name = "falcon")
    private String falcon;

    @Column(name = "avast")
    private String avast;

    @Column(name = "sentinelone")
    private String sentinelone;

    @Column(name = "detect_engine")
    private int detectEngine;
    @Column(name = "complete_engine")
    private int completeEngine;

    @Column(name = "score")
    private int score;
    @Column(name = "threat_label")
    private String threatLabel;

    @Column(columnDefinition = "TEXT", nullable = false, name = "report_url")
    private String reportUrl;
}
