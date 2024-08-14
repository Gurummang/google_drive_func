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
@Table(name = "file_status")
public class FileStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    @JoinColumn(name = "file_id", referencedColumnName = "id")
    private StoredFile storedFile;

    @Builder.Default
    @Column(name = "gscan_status")
    private int gscanStatus = -1;
    @Builder.Default
    @Column(name = "dlp_status")
    private int dlpStatus = -1;
    @Builder.Default
    @Column(name = "vt_status")
    private int vtStatus = -1;

}