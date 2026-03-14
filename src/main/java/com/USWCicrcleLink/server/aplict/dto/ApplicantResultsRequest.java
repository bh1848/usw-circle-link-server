package com.USWCicrcleLink.server.aplict.dto;


import com.USWCicrcleLink.server.aplict.domain.AplictStatus;
import com.USWCicrcleLink.server.global.validation.support.ValidationGroups;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Data
public class ApplicantResultsRequest {

    @NotNull(message = "지원서는 필수 입력값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    private UUID aplictUUID;

    @NotNull(message = "지원 상태는 필수 입력값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    private AplictStatus aplictStatus;
}
