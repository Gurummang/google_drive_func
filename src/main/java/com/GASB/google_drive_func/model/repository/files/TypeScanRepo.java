package com.GASB.google_drive_func.model.repository.files;

import com.GASB.google_drive_func.model.entity.FileUploadTable;
import com.GASB.google_drive_func.model.entity.TypeScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TypeScanRepo extends JpaRepository<TypeScan, Long> {


    @Query("SELECT COUNT(t) > 0 FROM TypeScan t WHERE t.file_upload = :fileUpload")
    boolean existsByUploadId(@Param("fileUpload")FileUploadTable obj);

}
