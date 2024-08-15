package com.GASB.google_drive_func.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "scan_table")
public class Gscan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne
    @JoinColumn(name = "file_id", nullable = false,referencedColumnName = "id")
    private StoredFile storedFile;

    @Column(name = "step2_detail", columnDefinition = "TEXT")
    private String step2Detail;


    @Column(name = "detect")
    private boolean detected;
}
