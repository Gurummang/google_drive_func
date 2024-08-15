package com.GASB.google_drive_func.model.repository.org;

import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.entity.Saas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgSaaSRepo extends JpaRepository<OrgSaaS, Integer> {

    @Query("SELECT spaceId FROM OrgSaaS WHERE id = :workspace_id")
    String getSpaceID(@Param("workspace_id")int workspace_id);
}
