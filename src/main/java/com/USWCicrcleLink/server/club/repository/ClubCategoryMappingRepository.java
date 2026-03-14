package com.USWCicrcleLink.server.club.repository;


import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.ClubCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Repository
public interface ClubCategoryMappingRepository
        extends JpaRepository<ClubCategoryMapping,Long> {
    @Query("SELECT cm FROM ClubCategoryMapping cm JOIN FETCH cm.clubCategory WHERE cm.club.clubId = :clubId")
    List<ClubCategoryMapping> findByClubClubId(@Param("clubId") Long clubId);

    @Modifying
    @Query("DELETE FROM ClubCategoryMapping cm WHERE cm.clubCategory.clubCategoryId = :clubCategoryId")
    void deleteByClubCategoryId(@Param("clubCategoryId") Long clubCategoryId);

    @Query("SELECT cm.club FROM ClubCategoryMapping cm WHERE cm.clubCategory.clubCategoryId IN :clubCategoryIds")
    List<Club> findClubsByCategoryIds(@Param("clubCategoryIds") List<Long> clubCategoryIds);

    @Query("SELECT DISTINCT cm.club FROM ClubCategoryMapping cm " +
            "WHERE cm.clubCategory.clubCategoryId IN :clubCategoryIds " +
            "AND cm.club.clubId IN :openClubIds")
    List<Club> findOpenClubsByCategoryIds(@Param("clubCategoryIds") List<Long> clubCategoryIds,
                                          @Param("openClubIds") List<Long> openClubIds);

    List<ClubCategoryMapping> findByClub_ClubId(Long clubId);

    @Transactional
    void deleteAllByClub_ClubIdAndClubCategory_ClubCategoryNameNotIn(Long clubId, Set<String> categoryNames);

}
