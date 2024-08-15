package com.GASB.google_drive_func.model.repository.channel;

import com.GASB.google_drive_func.model.entity.ChannelList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DirveChannelRepository extends JpaRepository<ChannelList, Long>{
    Optional<ChannelList> findByChannelId(String channelId);
    Optional<ChannelList> findByOrgSaasId(int orgSaasId);
    boolean existsByChannelId(String channelId);
}
