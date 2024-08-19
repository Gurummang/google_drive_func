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

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Activities a WHERE a.saasFileId = :saasFileId AND a.eventTs = :eventTs")
    boolean existsBySaasFileIdAndEventTs(@Param("saasFileId") String saasFileId, @Param("eventTs") LocalDateTime eventTs);

}
