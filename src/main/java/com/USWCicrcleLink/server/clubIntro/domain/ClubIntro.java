package com.USWCicrcleLink.server.clubIntro.domain;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.RecruitmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "CLUB_INTRO_TABLE")
public class ClubIntro {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_intro_id")
    private Long clubIntroId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;

    @Column(name = "club_intro", length=3000)
    private String clubIntro;

    @Column(name = "club_recruitment", length=3000)
    private String clubRecruitment;

    @Column(name = "googleForm_url")
    private String googleFormUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "club_intro_recruitment_status", nullable = false)
    private RecruitmentStatus recruitmentStatus = RecruitmentStatus.CLOSE;

    public void updateClubIntro(String clubIntro, String clubRecruitment , String googleFormUrl) {
        this.clubIntro = clubIntro;
        this.clubRecruitment = clubRecruitment;
        this.googleFormUrl = googleFormUrl;
    }

    public void toggleRecruitmentStatus() {
        // 현재 모집 상태와 반대로
        this.recruitmentStatus = this.recruitmentStatus.toggle();
    }
}