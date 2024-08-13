package com.GASB.google_drive_func.model.repository.org;

import com.GASB.google_drive_func.model.entity.OrgSaaS;
import com.GASB.google_drive_func.model.entity.Saas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrgSaaSRepo extends JpaRepository<OrgSaaS, Integer> {
//    OrgSaaS findByOrgId(String orgId);

//    Optional<OrgSaaS> findById(Long id);
//    Optional<OrgSaaS> findBySpaceId(String spaceId);
//    Optional<OrgSaaS> findByOrgIdAndSpaceId(int orgId, String spaceId);
//    Optional<OrgSaaS> findByOrgIdAndSaas(int orgId, Saas saas);
//    List<OrgSaaS> findAllByOrgIdAndSaas(int orgId, Saas saas);
    Optional<OrgSaaS> findBySpaceIdAndOrgId(String spaceId, int orgId);
    Optional<OrgSaaS> findBySpaceId(String spaceId);

    List<String> findSpaceIdByOrgIdAndSaas(int orgId, Saas saas);

    boolean existsBySpaceId(String spaceId);
}
