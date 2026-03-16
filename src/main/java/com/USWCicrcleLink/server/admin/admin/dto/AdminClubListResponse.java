package com.USWCicrcleLink.server.admin.admin.dto;

import com.USWCicrcleLink.server.club.domain.Department;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdminClubListResponse {
    private UUID clubUUID;
    private Department department;
    private String clubName;
    private String leaderName;
    private long numberOfClubMembers;
}
