package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.club.domain.ClubMembers;
import com.USWCicrcleLink.server.profile.domain.MemberType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

public class ClubMembersRepositoryImpl implements ClubMembersRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    // 동아리 회원과 프로필 조회
    @Override
    public List<ClubMembers> findAllWithProfileByClubClubId(Long clubId) {
        return em.createQuery(
                        "select cm from ClubMembers cm" +
                                " join fetch cm.profile p" +
                                " where cm.club.clubId = :clubId",
                        ClubMembers.class
                ).setParameter("clubId", clubId)
                .getResultList();
    }

    // 동아리 회원과 프로필 조회 가나다순
    @Override
    public List<ClubMembers> findAllWithProfileByName(Long clubId) {
        return em.createQuery(
                        "SELECT cm FROM ClubMembers cm" +
                                " JOIN FETCH cm.profile p" +
                                " WHERE cm.club.clubId = :clubId" +
                                " ORDER BY p.userName ASC",
                        ClubMembers.class
                ).setParameter("clubId", clubId)
                .getResultList();
    }

    // 동아리 회원과 프로필 조회 정회원, 비회원
    @Override
    public List<ClubMembers> findAllWithProfileByMemberType(Long clubId, MemberType memberType) {
        return em.createQuery(
                        "SELECT cm FROM ClubMembers cm" +
                                " JOIN FETCH cm.profile p" +
                                " WHERE cm.club.clubId = :clubId" +
                                " AND p.memberType = :memberType",
                        ClubMembers.class
                )
                .setParameter("clubId", clubId)
                .setParameter("memberType", memberType)
                .getResultList();
    }

    // 프로필로 속한 동아리 조회
    @Override
    public List<Long> findByProfileProfileIdsWithoutClub(List<Long> profileIds) {
        return em.createQuery(
                        "SELECT p.profileId FROM Profile p" +
                                " WHERE p.profileId IN :profileIds" +
                                " AND NOT EXISTS (SELECT 1 FROM ClubMembers cm WHERE cm.profile.profileId = p.profileId)",
                        Long.class
                ).setParameter("profileIds", profileIds)
                .getResultList();

    }
}
