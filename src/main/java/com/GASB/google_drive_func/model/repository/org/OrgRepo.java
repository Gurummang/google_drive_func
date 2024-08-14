package com.GASB.google_drive_func.model.repository.org;

import com.GASB.google_drive_func.model.entity.Org;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrgRepo extends JpaRepository<Org, Integer> {

}
