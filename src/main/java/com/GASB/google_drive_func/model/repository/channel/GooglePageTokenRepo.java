package com.GASB.google_drive_func.model.repository.channel;

import com.GASB.google_drive_func.model.entity.ChannelList;
import com.GASB.google_drive_func.model.entity.GooglePageToken;
import com.GASB.google_drive_func.model.entity.OrgSaaS;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GooglePageTokenRepo extends JpaRepository<GooglePageToken, Long> {


    @Query("SELECT g FROM GooglePageToken g WHERE g.channelId = :channelId")
    Optional<GooglePageToken> findObjByChannelId(@Param("channelId")String channelId);

    @Query("SELECT g.pageToken FROM GooglePageToken g WHERE g.channelId = :channelId")
    Optional<String> getPageTokenByChannelId(@Param("channelId") String channelId);

    @Transactional
    @Modifying
    @Query("UPDATE GooglePageToken g SET g.pageToken = :pageToken WHERE g.channelId = :channelId")
    void updatePageTokenByChannelId(@Param("channelId") String channelId, @Param("pageToken") String pageToken);

    @Query("SELECT g.orgSaaS.id FROM GooglePageToken g WHERE g.channelId = :channelId")
    int findByChannelId(@Param("channelId") String channelId);

    @Transactional
    @Modifying
    @Query("UPDATE GooglePageToken g SET g.lastAccessTime = CURRENT_TIMESTAMP WHERE g.channelId = :channelId")
    void updateLastAccessTimeByChannelId(@Param("channelId") String channelId);


}
