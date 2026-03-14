package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.club.domain.ClubHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface ClubHashtagRepository extends JpaRepository<ClubHashtag, Long> {
    @Query("SELECT ch FROM ClubHashtag ch WHERE ch.club.clubId IN :clubIds")
    List<ClubHashtag> findByClubIds(@Param("clubIds") List<Long> clubIds);

    @Query("SELECT ch FROM ClubHashtag ch WHERE ch.club.clubId = :clubId")
    List<ClubHashtag> findByClubClubId(@Param("clubId") Long clubId);

    @Query("SELECT ch.clubHashtag FROM ClubHashtag ch WHERE ch.club.clubId = :clubId")
    List<String> findHashtagsByClubId(@Param("clubId") Long clubId);

    @Transactional
    void deleteAllByClub_ClubIdAndClubHashtagNotIn(Long clubId, Set<String> hashtags);
}
