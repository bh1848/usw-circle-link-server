package com.USWCicrcleLink.server.clubApplication.dto;

import com.USWCicrcleLink.server.profile.domain.Profile;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicantsResponse {

    private UUID clubApplicationUUID;
    private String userName;
    private String major;
    private String studentNumber;
    private String userHp;

    public ApplicantsResponse(UUID clubApplicationUUID, Profile profile) {
        this.clubApplicationUUID = clubApplicationUUID;
        this.userName = profile.getUserName();
        this.major = profile.getMajor();
        this.studentNumber = profile.getStudentNumber();
        this.userHp = profile.getUserHp();
    }
}
