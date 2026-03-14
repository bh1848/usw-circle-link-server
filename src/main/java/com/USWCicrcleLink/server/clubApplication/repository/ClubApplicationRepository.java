package com.USWCicrcleLink.server.clubApplication.repository;

import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.profile.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClubApplicationRepository extends JpaRepository<ClubApplication, Long>, ClubApplicationRepositoryCustom {
    List<ClubApplication> findByProfileProfileId(Long profileId);

    Optional<ClubApplication> findByClub_ClubIdAndClubApplicationUUIDAndChecked(Long clubId, UUID clubApplicationUUID, boolean checked);

    List<ClubApplication> findByClub_ClubIdAndChecked(Long clubId, boolean checked);

    Optional<ClubApplication> findByClub_ClubIdAndClubApplicationUUIDAndCheckedAndClubApplicationStatus(Long clubId, UUID clubApplicationUUID, boolean checked, ClubApplicationStatus status);
    List<ClubApplication> findAllByDeleteDateBefore(LocalDateTime dateTime);

    void deleteAllByProfile(Profile profile);

    @Query("SELECT COUNT(clubApplication) > 0 FROM ClubApplication clubApplication WHERE clubApplication.profile = :profile AND clubApplication.club.clubUUID = :clubUUID")
    boolean existsByProfileAndClubUUID(Profile profile, UUID clubUUID);

    @Query("SELECT clubApplication.club FROM ClubApplication clubApplication WHERE clubApplication.clubApplicationId = :clubApplicationId")
    Optional<Club> findClubByClubApplicationId(@Param("clubApplicationId") Long clubApplicationId);
}
