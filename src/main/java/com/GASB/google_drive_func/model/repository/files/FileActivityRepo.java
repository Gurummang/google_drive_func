package com.GASB.google_drive_func.model.repository.files;

import com.GASB.google_drive_func.model.entity.Activities;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface FileActivityRepo extends JpaRepository<Activities, Long>{
    Optional<Activities> findBysaasFileId(String fileId);

    Optional<Activities> findBySaasFileIdAndEventTs(String fileId, LocalDateTime eventTs);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN TRUE ELSE FALSE END FROM Activities a WHERE a.saasFileId = :id AND a.eventTs = :eventTs")
    boolean existsObjectById(@Param("id") String saasFileId, @Param("eventTs") LocalDateTime eventTs);

    boolean existsBySaasFileIdAndEventTs(String saasFileId, LocalDateTime eventTs);

}
