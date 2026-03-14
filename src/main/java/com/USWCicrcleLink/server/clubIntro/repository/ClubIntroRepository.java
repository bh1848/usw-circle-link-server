package com.USWCicrcleLink.server.clubIntro.repository;

import com.USWCicrcleLink.server.clubIntro.domain.ClubIntro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClubIntroRepository extends JpaRepository<ClubIntro, Long> {
    @Query("SELECT ci FROM ClubIntro ci WHERE ci.club.clubId = :clubId")
    Optional<ClubIntro> findByClubClubId(@Param("clubId") Long clubId);

    @Query("SELECT ci.club.clubId FROM ClubIntro ci WHERE ci.recruitmentStatus = 'OPEN'")
    List<Long> findOpenClubIds();

    @Query("SELECT ci FROM ClubIntro ci WHERE ci.club.clubUUID = :clubUUID")
    Optional<ClubIntro> findByClubUUID(@Param("clubUUID") UUID clubUUID);
}
