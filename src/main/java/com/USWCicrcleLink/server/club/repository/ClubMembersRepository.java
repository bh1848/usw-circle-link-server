package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.ClubMembers;
import com.USWCicrcleLink.server.profile.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClubMembersRepository extends JpaRepository<ClubMembers,Long>, ClubMembersRepositoryCustom {

    //동아리원 조회 성능 비교
    List<ClubMembers> findByClub(Club club);
    List<ClubMembers> findByProfileProfileId(Long profileId);
    Optional<ClubMembers> findByProfileProfileIdAndClubClubId(Long profileId, Long clubId);
    Optional<ClubMembers> findByClubClubIdAndClubMemberUUID(Long clubId, UUID clubMemberUUID);
    List<ClubMembers> findByClubClubIdAndClubMemberUUIDIn(Long clubId, List<UUID> clubMemberUUIDs);

    void deleteAllByProfile(Profile profile);

    @Query("SELECT COUNT(cm) > 0 FROM ClubMembers cm WHERE cm.profile = :profile AND cm.club.clubUUID = :clubUUID")
    boolean existsByProfileAndClubUUID(Profile profile, UUID clubUUID);

    @Query("SELECT cm.profile FROM ClubMembers cm WHERE cm.club.clubUUID = :clubUUID")
    List<Profile> findProfilesByClubUUID(UUID clubUUID);

    @Query("SELECT cm.club.clubUUID FROM ClubMembers cm WHERE cm.profile.profileId = :profileId")
    List<UUID> findClubUUIDsByProfileId(@Param("profileId") Long profileId);
}
