package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.admin.admin.dto.AdminClubListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClubRepositoryCustom {
    Page<AdminClubListResponse> findAllWithMemberAndLeaderCount(Pageable pageable);

    void deleteClubAndDependencies(Long clubId);
}
