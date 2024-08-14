package com.GASB.google_drive_func.model.repository.org;

import com.GASB.google_drive_func.model.entity.Saas;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SaasRepo extends JpaRepository<Saas,Long> {

    @Query("select s from Saas s where s.id = :saas_id")
    Optional<Saas> findById(@Param("saas_id") int saas_id);
    Optional<Saas> findBySaasName(String saasName);

}
