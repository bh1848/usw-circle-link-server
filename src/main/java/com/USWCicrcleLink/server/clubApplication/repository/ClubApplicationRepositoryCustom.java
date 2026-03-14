package com.USWCicrcleLink.server.clubApplication.repository;

import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;

import java.util.List;

public interface ClubApplicationRepositoryCustom {
    List<ClubApplication> findAllWithProfileByClubId(Long clubId, boolean checked);

    List<ClubApplication> findAllWithProfileByClubIdAndFailed(Long clubId, boolean checked, ClubApplicationStatus status);
}
