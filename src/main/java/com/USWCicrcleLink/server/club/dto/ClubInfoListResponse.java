package com.USWCicrcleLink.server.club.dto;

import com.USWCicrcleLink.server.club.domain.Club;
import lombok.*;

import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClubInfoListResponse {
    private UUID clubUUID;
    private String clubName;
    private String mainPhoto;

    public ClubInfoListResponse(Club club, String mainPhoto) {
        this.clubUUID = club.getClubUUID();
        this.clubName = club.getClubName();
        this.mainPhoto = mainPhoto;
    }
}

