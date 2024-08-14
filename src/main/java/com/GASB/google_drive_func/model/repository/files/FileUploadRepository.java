package com.GASB.google_drive_func.model.repository.files;

import com.GASB.google_drive_func.model.dto.file.SlackRecentFileDTO;
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
    @Query("SELECT new com.GASB.google_drive_func.model.dto.file.SlackRecentFileDTO(a.fileName, u.userName, sf.type, fu.timestamp) " +
            "FROM FileUploadTable fu " +
            "JOIN OrgSaaS os ON fu.orgSaaS.id = os.id " +
            "JOIN Activities a ON fu.saasFileId = a.saasFileId " +
            "JOIN StoredFile sf ON fu.hash = sf.saltedHash " +
            "JOIN MonitoredUsers u ON a.user.userId = u.userId " +
            "WHERE os.org.id = :orgId AND os.saas.id = :saasId " +
            "ORDER BY fu.timestamp DESC LIMIT 10")
    List<SlackRecentFileDTO> findRecentFilesByOrgIdAndSaasId(@Param("orgId") int orgId, @Param("saasId") int saasId);

    // Corrected method to find by OrgSaaS fields

    Optional<FileUploadTable> findBySaasFileIdAndTimestamp(String saasFileId, LocalDateTime timestamp);

    @Query("UPDATE FileUploadTable fu " +
            "SET fu.deleted = true " +
            "WHERE fu.saasFileId = :saasFileId")
    void checkDelete(@Param("saasFileId") String saasFileId);


    @Query("SELECT CASE WHEN COUNT(fu) > 0 THEN TRUE ELSE FALSE END FROM FileUploadTable fu WHERE fu.saasFileId = :id AND fu.timestamp = :timestamp")
    boolean existsObjectById(@Param("id") String SaasFileId, @Param("timestamp") LocalDateTime timestamp);


    boolean existsBySaasFileIdAndTimestamp(String saasFileId, LocalDateTime timestamp);
}
