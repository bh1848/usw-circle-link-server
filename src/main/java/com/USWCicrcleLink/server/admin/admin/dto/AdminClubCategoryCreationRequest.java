package com.USWCicrcleLink.server.admin.admin.dto;

import com.USWCicrcleLink.server.global.validation.support.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdminClubCategoryCreationRequest {

    @NotBlank(message = "카테고리 이름은 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 1, max = 20, message = "카테고리는 최대 20자까지 입력 가능합니다.", groups = ValidationGroups.SizeGroup.class)
    private String clubCategoryName;
}
