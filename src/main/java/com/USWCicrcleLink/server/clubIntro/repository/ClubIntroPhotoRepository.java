package com.USWCicrcleLink.server.clubIntro.repository;

import com.USWCicrcleLink.server.clubIntro.domain.ClubIntro;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntroPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubIntroPhotoRepository extends JpaRepository<ClubIntroPhoto, Long> {
    Optional<ClubIntroPhoto> findByClubIntro_ClubIntroIdAndOrder(Long clubIntroId, int order);

    List<ClubIntroPhoto> findByClubIntro(ClubIntro clubIntro);

    @Query("SELECT cip FROM ClubIntroPhoto cip WHERE cip.clubIntro.club.clubId = :clubId ORDER BY cip.order")
    List<ClubIntroPhoto> findByClubIntroClubId(@Param("clubId") Long clubId);
}
