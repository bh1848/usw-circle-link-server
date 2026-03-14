package com.USWCicrcleLink.server.aplict.repository;

import com.USWCicrcleLink.server.aplict.domain.Aplict;
import com.USWCicrcleLink.server.aplict.domain.AplictStatus;
import com.USWCicrcleLink.server.club.club.domain.Club;
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
public interface AplictRepository extends JpaRepository<Aplict, Long>, AplictRepositoryCustom {
    List<Aplict> findByProfileProfileId(Long profileId);

    Optional<Aplict> findByClub_ClubIdAndAplictUUIDAndChecked(Long clubId, UUID aplictUUID, boolean checked);

    List<Aplict> findByClub_ClubIdAndChecked(Long clubId, boolean checked);

    Optional<Aplict> findByClub_ClubIdAndAplictUUIDAndCheckedAndAplictStatus(Long clubId, UUID aplictUUID, boolean checked, AplictStatus status);
    List<Aplict> findAllByDeleteDateBefore(LocalDateTime dateTime);

    void deleteAllByProfile(Profile profile);

    @Query("SELECT COUNT(a) > 0 FROM Aplict a WHERE a.profile = :profile AND a.club.clubUUID = :clubUUID")
    boolean existsByProfileAndClubUUID(Profile profile, UUID clubUUID);

    @Query("SELECT a.club FROM Aplict a WHERE a.aplictId = :aplictId")
    Optional<Club> findClubByAplictId(@Param("aplictId") Long aplictId);
}
