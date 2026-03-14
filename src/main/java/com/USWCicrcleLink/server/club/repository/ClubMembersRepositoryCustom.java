package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.club.domain.ClubMembers;

import com.USWCicrcleLink.server.profile.domain.MemberType;

import java.util.List;

public interface ClubMembersRepositoryCustom {
    List<ClubMembers> findAllWithProfileByClubClubId(Long clubId);
    List<ClubMembers> findAllWithProfileByName(Long clubId);
    List<ClubMembers> findAllWithProfileByMemberType(Long clubId, MemberType memberType);
    List<Long> findByProfileProfileIdsWithoutClub(List<Long> profileIds);
}
