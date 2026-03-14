package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.club.domain.Club;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long>, ClubRepositoryCustom {
    @NonNull
    Page<Club> findAll(@NonNull Pageable pageable);

    boolean existsByClubName(String clubName);
    Optional<Club> findByClubUUID(UUID clubUUID);

    @Query("SELECT c FROM Club c WHERE c.clubId IN :clubIds")
    List<Club> findByClubIds(@Param("clubIds") List<Long> clubIds);

    @Query("SELECT c.clubId FROM Club c WHERE c.clubUUID = :clubUUID")
    Optional<Long> findClubIdByClubUUID(@Param("clubUUID") UUID clubUUID);

    boolean existsByClubRoomNumber(String clubRoomNumber);
}
