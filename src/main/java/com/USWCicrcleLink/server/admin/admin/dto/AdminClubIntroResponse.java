package com.USWCicrcleLink.server.admin.admin.dto;

import com.USWCicrcleLink.server.club.domain.RecruitmentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;


@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdminClubIntroResponse {
    private UUID clubUUID;
    private String mainPhoto;
    private List<String> introPhotos;
    private String clubName;
    private String leaderName;
    private String leaderHp;
    private String clubInsta;
    private String clubIntro;
    private RecruitmentStatus recruitmentStatus;
    private String googleFormUrl;
    private List<String> clubHashtags;
    private List<String> clubCategoryNames;
    private String clubRoomNumber;
    private String clubRecruitment;
}