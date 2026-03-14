package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.club.domain.ClubCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClubCategoryRepository extends JpaRepository<ClubCategory, Long> {

    boolean existsByClubCategoryName(String clubCategoryName);

    Optional<ClubCategory> findByClubCategoryUUID(UUID clubCategoryUUID);

    @Query("SELECT c.clubCategoryId FROM ClubCategory c WHERE c.clubCategoryUUID IN :clubCategoryUUIDs")
    List<Long> findClubCategoryIdsByUUIDs(@Param("clubCategoryUUIDs") List<UUID> clubCategoryUUIDs);

    Optional<ClubCategory> findByClubCategoryName(String clubCategoryName);
}
