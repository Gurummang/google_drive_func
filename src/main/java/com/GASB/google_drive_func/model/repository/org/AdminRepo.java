package com.GASB.google_drive_func.model.repository.org;

import com.GASB.google_drive_func.model.entity.AdminUsers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepo extends JpaRepository<AdminUsers, Integer> {


//    Optional<AdminUsers> findByEmail(String email);
//    Optional<AdminUsers> findByEmailAndPassword(String email, String password);
//    Optional<AdminUsers> findByEmailAndOrgId(String email, int orgId);

    boolean existsByEmail(String email);

    @Query("SELECT a FROM AdminUsers a WHERE a.email = :email")
    Optional<AdminUsers> findByEmail(@Param("email") String email);

}
