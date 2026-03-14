package com.USWCicrcleLink.server.user.dto;

import com.USWCicrcleLink.server.club.domain.FloorPhotoEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClubFloorPhotoResponse {
    private FloorPhotoEnum roomFloor;
    private String floorPhotoPath;
}
