package com.GASB.google_drive_func.model.repository.files;

import com.GASB.google_drive_func.model.entity.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findBySaltedHash(String saltedHash);


    boolean existsBySaltedHash(String saltedHash);

    @Query("SELECT SUM(sf.size) FROM Org o " +
            "INNER JOIN OrgSaaS os ON o.id = os.org.id " +
            "INNER JOIN Saas s ON os.saas.id = s.id " +
            "INNER JOIN FileUploadTable fu ON os.id = fu.orgSaaS.id " +
            "INNER JOIN StoredFile sf ON fu.hash = sf.saltedHash " +
            "WHERE o.id = :orgId AND s.id = :saasId AND fu.deleted != true")
    Long getTotalFileSize(@Param("orgId") int orgId, @Param("saasId") int saasId);

    @Query("SELECT SUM(sf.size) FROM Org o " +
            "INNER JOIN OrgSaaS os ON o.id = os.org.id " +
            "INNER JOIN Saas s ON os.saas.id = s.id " +
            "INNER JOIN FileUploadTable fu ON os.id = fu.orgSaaS.id " +
            "INNER JOIN StoredFile sf ON fu.hash = sf.saltedHash " +
            "INNER JOIN VtReport vr ON sf.id = vr.storedFile.id " +
            "WHERE o.id = :orgId AND s.id = :saasId AND vr.threatLabel != 'none' AND fu.deleted != true")
    Long getTotalMaliciousFileSize(@Param("orgId") int orgId, @Param("saasId") int saasId);

    @Query("SELECT SUM(sf.size) FROM Org o " +
            "INNER JOIN OrgSaaS os ON o.id = os.org.id " +
            "INNER JOIN Saas s ON os.saas.id = s.id " +
            "INNER JOIN FileUploadTable fu ON os.id = fu.orgSaaS.id " +
            "INNER JOIN StoredFile sf ON fu.hash = sf.saltedHash " +
            "INNER JOIN DlpReport dr ON sf.id = dr.storedFile.id " +
            "WHERE o.id = :orgId AND s.id = :saasId AND dr.dlp = true AND fu.deleted != true")
    Long getTotalDlpFileSize(@Param("orgId") int orgId, @Param("saasId") int saasId);



    @Query("SELECT COUNT(DISTINCT fu.id) FROM Org o "+
            "INNER JOIN OrgSaaS os ON o.id = os.org.id "+
            "INNER JOIN Saas s ON os.saas.id = s.id "+
            "INNER JOIN FileUploadTable fu ON os.id = fu.orgSaaS.id "+
            "WHERE o.id = :orgId AND s.id = :saasId AND fu.deleted != true")
    int countTotalFiles(@Param("orgId") int orgId, @Param("saasId") int saasId);

    @Query("SELECT COUNT(DISTINCT fu.id) FROM Org o " +
            "INNER JOIN OrgSaaS os ON o.id = os.org.id " +
            "INNER JOIN Saas s ON os.saas.id = s.id " +
            "INNER JOIN FileUploadTable fu ON os.id = fu.orgSaaS.id " +
            "INNER JOIN DlpReport dr ON fu.hash = dr.storedFile.saltedHash " +
            "WHERE o.id = :orgId AND s.id = :saasId AND dr.dlp = true AND fu.deleted != true")
    int countSensitiveFiles(@Param("orgId") int orgId, @Param("saasId") int saasId);

    @Query("SELECT COUNT(DISTINCT fu.id) FROM Org o " +
            "INNER JOIN OrgSaaS os ON o.id = os.org.id " +
            "INNER JOIN Saas s ON os.saas.id = s.id " +
            "INNER JOIN FileUploadTable fu ON os.id = fu.orgSaaS.id " +
            "INNER JOIN VtReport vr ON fu.hash = vr.storedFile.saltedHash " +
            "WHERE o.id = :orgId AND s.id = :saasId AND vr.threatLabel != 'none' AND fu.deleted != true")
    int countMaliciousFiles(@Param("orgId") int orgId, @Param("saasId") int saasId);

    @Query("SELECT COUNT(DISTINCT mu.userId) FROM Org o " +
            "INNER JOIN OrgSaaS os ON o.id = os.org.id " +
            "INNER JOIN Saas s ON os.saas.id = s.id " +
            "INNER JOIN MonitoredUsers mu ON os.id = mu.orgSaaS.id " +
            "WHERE o.id = :orgId AND s.id = :saasId")
    int countConnectedAccounts(@Param("orgId") int orgId, @Param("saasId") int saasId);
}
