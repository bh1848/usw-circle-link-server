package com.USWCicrcleLink.server.clubLeader.dto.club;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.RecruitmentStatus;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntro;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LeaderClubIntroResponse {

    private UUID clubUUID;

    private String clubIntro;

    private String clubRecruitment;

    private RecruitmentStatus recruitmentStatus;

    private String googleFormUrl;

    private List<String> introPhotos;

    public LeaderClubIntroResponse(Club club, ClubIntro clubIntro, List<String> introPhotoUrls) {
        this.clubUUID = club.getClubUUID();
        this.clubIntro = clubIntro.getClubIntro();
        this.clubRecruitment = clubIntro.getClubRecruitment();
        this.recruitmentStatus = clubIntro.getRecruitmentStatus();
        this.googleFormUrl = clubIntro.getGoogleFormUrl();
        this.introPhotos = introPhotoUrls;
    }
}
