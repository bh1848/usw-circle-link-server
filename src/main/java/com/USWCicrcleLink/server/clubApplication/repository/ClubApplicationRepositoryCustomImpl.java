package com.USWCicrcleLink.server.clubApplication.repository;

import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ClubApplicationRepositoryCustomImpl implements ClubApplicationRepositoryCustom {

    @PersistenceContext
    private final EntityManager em;

    @Override
    public List<ClubApplication> findAllWithProfileByClubId(Long clubId, boolean checked) {
        return em.createQuery(
                        "SELECT clubApplication FROM ClubApplication clubApplication JOIN FETCH clubApplication.profile profile" +
                                " WHERE clubApplication.club.clubId = :clubId AND clubApplication.checked = :checked",
                        ClubApplication.class
                ).setParameter("clubId", clubId)
                .setParameter("checked", checked)
                .getResultList();
    }

    @Override
    public List<ClubApplication> findAllWithProfileByClubIdAndFailed(Long clubId, boolean checked, ClubApplicationStatus status) {
        return em.createQuery(
                        "SELECT clubApplication FROM ClubApplication clubApplication JOIN FETCH clubApplication.profile profile" +
                                " WHERE clubApplication.club.clubId = :clubId AND clubApplication.checked = :checked AND clubApplication.clubApplicationStatus = :status",
                        ClubApplication.class
                ).setParameter("clubId", clubId)
                .setParameter("checked", checked)
                .setParameter("status", status)
                .getResultList();
    }
}
