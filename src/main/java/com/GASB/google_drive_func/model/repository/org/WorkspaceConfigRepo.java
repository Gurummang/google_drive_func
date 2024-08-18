package com.GASB.google_drive_func.model.repository.org;

import com.GASB.google_drive_func.model.entity.WorkspaceConfig;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkspaceConfigRepo extends JpaRepository<WorkspaceConfig, String> {
    @Query("select w from WorkspaceConfig w where w.id = :id")
    Optional<WorkspaceConfig> findById(@Param("id") int id);

    // Update Token
    @Modifying
    @Transactional
    @Query("update WorkspaceConfig w set w.token = :token where w.id = :id")
    void updateToken(@Param("id") int id,@Param("token") String token);
}
