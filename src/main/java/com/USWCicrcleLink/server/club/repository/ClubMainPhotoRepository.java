package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.ClubMainPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubMainPhotoRepository extends JpaRepository<ClubMainPhoto, Long> {
    ClubMainPhoto findByClub_ClubId(Long clubId);

    Optional<ClubMainPhoto> findByClub(Club club);

    @Query("SELECT cmp FROM ClubMainPhoto cmp WHERE cmp.club.clubId = :clubId")
    Optional<ClubMainPhoto> findByClubClubId(@Param("clubId") Long clubId);

    @Query("SELECT cmp FROM ClubMainPhoto cmp WHERE cmp.club.clubId IN :clubIds")
    List<ClubMainPhoto> findByClubIds(@Param("clubIds") List<Long> clubIds);

    @Query("SELECT c.clubMainPhotoS3Key FROM ClubMainPhoto c WHERE c.club.clubId = :clubId")
    Optional<String> findS3KeyByClubId(@Param("clubId") Long clubId);
}
