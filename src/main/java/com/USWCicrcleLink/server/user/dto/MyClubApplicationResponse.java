package com.USWCicrcleLink.server.user.dto;

import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyClubApplicationResponse {

    private UUID clubUUID;
    private String mainPhotoPath;
    private String clubName;
    private String leaderName;
    private String leaderHp;
    private String clubInsta;
    private String clubRoomNumber;
    private ClubApplicationStatus clubApplicationStatus;
}
