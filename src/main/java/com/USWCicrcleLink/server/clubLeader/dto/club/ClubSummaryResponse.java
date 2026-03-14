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
public class ClubSummaryResponse {

    // club
    private UUID clubUUID;
    private String clubName;
    private String leaderName;
    private String leaderHp;
    private String clubInsta;
    private String clubRoomNumber;

    // clubHashtag
    private List<String> clubHashtag;

    // club
    private List<String> clubCategories;

    // clubIntro
    private String clubIntro;
    private String clubRecruitment;
    private RecruitmentStatus recruitmentStatus;
    private String googleFormUrl;

    // photo
    private String mainPhoto;
    private List<String> introPhotos;

    public ClubSummaryResponse(Club club, List<String> clubHashtag, List<String> clubCategories,
                               ClubIntro clubIntro, String mainPhotoUrl, List<String> introPhotoUrls) {
        // club
        this.clubUUID = club.getClubUUID();
        this.clubName = club.getClubName();
        this.leaderName = club.getLeaderName();
        this.leaderHp = club.getLeaderHp();
        this.clubInsta = club.getClubInsta();
        this.clubRoomNumber = club.getClubRoomNumber();
        // clubHashtag
        this.clubHashtag = clubHashtag;
        // clubCategories
        this.clubCategories = clubCategories;
        // clubIntro
        this.clubIntro = clubIntro.getClubIntro();
        this.clubRecruitment = clubIntro.getClubRecruitment();
        this.recruitmentStatus = clubIntro.getRecruitmentStatus();
        this.googleFormUrl = clubIntro.getGoogleFormUrl();
        // photo
        this.mainPhoto = mainPhotoUrl;
        this.introPhotos = introPhotoUrls;
    }
}
