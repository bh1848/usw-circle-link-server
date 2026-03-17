package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.admin.admin.dto.AdminClubListResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public class ClubRepositoryCustomImpl implements ClubRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public Page<AdminClubListResponse> findAllWithMemberAndLeaderCount(Pageable pageable) {
        String jpql = "SELECT new com.USWCicrcleLink.server.admin.admin.dto.AdminClubListResponse(" +
                "c.clubUUID, c.department, c.clubName, c.leaderName, " +
                "(COUNT(DISTINCT cm.clubMemberId) + MAX(CASE WHEN l IS NOT NULL THEN 1 ELSE 0 END))) " +
                "FROM Club c " +
                "LEFT JOIN ClubMembers cm ON cm.club.clubId = c.clubId " +
                "LEFT JOIN Leader l ON l.club.clubId = c.clubId " +
                "GROUP BY c.clubId, c.clubUUID, c.department, c.clubName, c.leaderName";

        String countJpql = "SELECT COUNT(c) FROM Club c";

        TypedQuery<AdminClubListResponse> query = em.createQuery(jpql, AdminClubListResponse.class);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        Long totalCount = em.createQuery(countJpql, Long.class).getSingleResult();
        List<AdminClubListResponse> results = query.getResultList();

        return new PageImpl<>(results, pageable, totalCount);
    }

    @Override
    public void deleteClubAndDependencies(Long clubId) {
        em.createQuery("DELETE FROM ClubMemberAccountStatus cmas WHERE cmas.club.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();

        em.createQuery("DELETE FROM ClubHashtag ch WHERE ch.club.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();

        em.createQuery("DELETE FROM ClubCategoryMapping cm WHERE cm.club.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();

        em.createQuery("DELETE FROM ClubMembers cm WHERE cm.club.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();

        em.createQuery("DELETE FROM ClubApplication clubApplication WHERE clubApplication.club.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();

        em.createQuery("DELETE FROM ClubIntroPhoto cip WHERE cip.clubIntro.club.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();

        em.createQuery("DELETE FROM ClubMainPhoto cmp WHERE cmp.club.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();

        em.createQuery("DELETE FROM ClubIntro ci WHERE ci.club.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();

        em.createQuery("DELETE FROM Leader l WHERE l.club.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();

        em.createQuery("DELETE FROM Club c WHERE c.clubId = :clubId")
                .setParameter("clubId", clubId)
                .executeUpdate();
    }
}
