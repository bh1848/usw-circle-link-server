package com.USWCicrcleLink.server.club.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClubCategoryResponse {
    private UUID clubCategoryUUID;
    private String clubCategoryName;
}

