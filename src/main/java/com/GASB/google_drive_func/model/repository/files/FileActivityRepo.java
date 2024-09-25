package com.GASB.google_drive_func.model.repository.files;

import com.GASB.google_drive_func.model.entity.Activities;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FileActivityRepo extends JpaRepository<Activities, Long>{

    @Query("SELECT CASE WHEN EXISTS (SELECT 1 FROM Activities a WHERE a.saasFileId = :saasFileId AND a.eventTs = :eventTs) THEN true ELSE false END")
    boolean existsBySaasFileIdAndEventTs(@Param("saasFileId") String saasFileId, @Param("eventTs") LocalDateTime eventTs);

    @Query("SELECT a FROM Activities a WHERE a.saasFileId = :saasFileId ORDER BY a.eventTs DESC LIMIT 1")
    Optional<Activities> findRecentBySaasFileId(@Param("saasFileId") String saasFileId);

    @Query("SELECT COUNT(a) > 0 FROM Activities a WHERE a.saasFileId = :saasFileId")
    boolean existsBySaaSFileId(@Param("saasFileId") String saasFileId);

}
