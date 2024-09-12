package com.GASB.google_drive_func.model.repository.files;

import com.GASB.google_drive_func.model.dto.file.DriveRecentFileDTO;
import com.GASB.google_drive_func.model.entity.FileUploadTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface FileUploadRepository extends JpaRepository<FileUploadTable, Long> {
    @Query("SELECT new com.GASB.google_drive_func.model.dto.file.DriveRecentFileDTO(a.fileName, u.userName, sf.type, fu.timestamp) " +
            "FROM FileUploadTable fu " +
            "JOIN OrgSaaS os ON fu.orgSaaS.id = os.id " +
            "JOIN Activities a ON fu.saasFileId = a.saasFileId " +
            "JOIN StoredFile sf ON fu.hash = sf.saltedHash " +
            "JOIN MonitoredUsers u ON a.user.id = u.id " +
            "WHERE os.org.id = :orgId AND os.saas.id = :saasId " +
            "ORDER BY fu.timestamp DESC LIMIT 10")
    List<DriveRecentFileDTO> findRecentFilesByOrgIdAndSaasId(@Param("orgId") int orgId, @Param("saasId") int saasId);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FileUploadTable f WHERE f.saasFileId = :saasFileId AND f.timestamp = :timestamp")
    boolean existsBySaasFileIdAndTimestamp(@Param("saasFileId") String saasFileId, @Param("timestamp") LocalDateTime timestamp);

    @Query("SELECT fu FROM FileUploadTable fu WHERE fu.hash = :file_hash AND fu.id = :idx")
    Optional<FileUploadTable> findByIdAndFileHash(@Param("idx") int idx, @Param("file_hash")String file_hash);


}
