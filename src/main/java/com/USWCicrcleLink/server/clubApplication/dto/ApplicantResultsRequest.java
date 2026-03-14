package com.USWCicrcleLink.server.clubApplication.dto;

import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import com.USWCicrcleLink.server.global.validation.support.ValidationGroups;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ApplicantResultsRequest {

    @NotNull(message = "지원서는 필수 입력값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    private UUID clubApplicationUUID;

    @NotNull(message = "지원 상태는 필수 입력값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    private ClubApplicationStatus clubApplicationStatus;
}
