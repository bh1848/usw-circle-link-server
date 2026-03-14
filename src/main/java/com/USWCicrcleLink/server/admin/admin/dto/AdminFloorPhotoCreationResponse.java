package com.USWCicrcleLink.server.admin.admin.dto;

import com.USWCicrcleLink.server.club.domain.FloorPhotoEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdminFloorPhotoCreationResponse {
    private FloorPhotoEnum floor;
    private String presignedUrl;
}
