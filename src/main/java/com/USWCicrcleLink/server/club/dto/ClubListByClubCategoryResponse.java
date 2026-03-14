package com.USWCicrcleLink.server.club.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClubListByClubCategoryResponse {
    private UUID clubCategoryUUID;
    private String clubCategoryName;
    private List<ClubListResponse> clubs;
}
