package com.GASB.google_drive_func.model.repository;

import com.GASB.google_drive_func.model.entity.MonitoredUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitoredUserRepo extends JpaRepository<MonitoredUsers, Long> {
    Optional<MonitoredUsers> findByUserId(String userId);

    boolean existsByUserId(String userId);

    @Query("SELECT u FROM MonitoredUsers u WHERE u.email = :userName")
    Optional<MonitoredUsers> findByEmail(String userName);

    @Query("SELECT u FROM MonitoredUsers u WHERE u.email = :email AND u.orgSaaS.id = :orgSaaSId")
    Optional<MonitoredUsers> findByEmailAndOrgSaaS(@Param("email") String email,@Param("orgSaaSId") int orgSaaSId);

    @Query("SELECT u FROM MonitoredUsers u WHERE u.userId = :user_id")
    Optional<MonitoredUsers> fineByUserId(@Param("user_id") String userId);

    //DISTINCT : 중복된 값을 제거하는 키워드이다.
    //근데 JPQL 에서는 DISTINCT를 사용할 수 없다.
    // 그래서 nativeQuery = true로 설정하고 사용한다.
//    @Query(nativeQuery = true, value =
//            "SELECT " +
//                    "    u.user_name AS userName, " +
//                    "    COUNT(DISTINCT CASE WHEN dr.dlp = TRUE THEN fu.id END) AS sensitiveFilesCount, " +
//                    "    COUNT(DISTINCT CASE WHEN vr.threat_label != 'none' THEN fu.id END) AS maliciousFilesCount, " +
//                    "    MAX(fu.upload_ts) AS lastUploadedTimestamp " +
//                    "FROM " +
//                    "    monitored_users u " +
//                    "JOIN " +
//                    "    org_saas os ON u.org_saas_id = os.id " +
//                    "JOIN " +
//                    "    file_upload fu ON u.org_saas_id = fu.org_saas_id " +
//                    "LEFT JOIN " +
//                    "    dlp_report dr ON fu.salted_hash = dr.file_id " +
//                    "LEFT JOIN " +
//                    "    vt_report vr ON fu.salted_hash = vr.file_id " +
//                    "WHERE " +
//                    "    os.org_id = :orgId " +
//                    "    AND os.saas_id = :saasId " +
//                    "GROUP BY " +
//                    "    u.user_name " +
//                    "ORDER BY " +
//                    "    (3 * COUNT(DISTINCT CASE WHEN vr.threat_label != 'none' THEN fu.id END) + COUNT(DISTINCT CASE WHEN dr.dlp = TRUE THEN fu.id END)) DESC " +
//                    "LIMIT 5")
//    List<Object[]> findTopUsers(@Param("orgId") int orgId, @Param("saasId") int saasId);
}
