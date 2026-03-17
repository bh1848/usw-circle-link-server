package com.USWCicrcleLink.server.admin.admin.dto;

import com.USWCicrcleLink.server.club.domain.Department;
import com.USWCicrcleLink.server.global.validation.annotation.Sanitize;
import com.USWCicrcleLink.server.global.validation.annotation.ValidClubRoomNumber;
import com.USWCicrcleLink.server.global.validation.support.ValidationGroups;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdminClubCreationRequest {

    @NotBlank(message = "아이디는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 5, max = 20, message = "아이디는 5~20자 이내여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문 대/소문자, 숫자만 포함할 수 있으며 공백을 포함할 수 없습니다.", groups = ValidationGroups.PatternGroup.class)
    private String leaderAccount;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자 이내여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])(?!.*\\s).*$",
            message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 하며 공백을 포함할 수 없습니다.",
            groups = ValidationGroups.PatternGroup.class
    )
    private String leaderPw;

    @NotBlank(message = "비밀번호 확인은 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    private String leaderPwConfirm;

    @NotBlank(message = "동아리명은 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @Sanitize
    @Size(min = 1, max = 10, message = "동아리명은 최대 10자까지 입력 가능합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^[가-힣a-zA-Z0-9]+$", message = "동아리명에는 한글, 영문 대소문자, 숫자만 포함할 수 있으며 공백 또는 특수문자를 포함할 수 없습니다.", groups = ValidationGroups.PatternGroup.class)
    private String clubName;

    @NotNull(message = "학부는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @Enumerated(EnumType.STRING)
    private Department department;

    @NotBlank(message = "운영자 비밀번호는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    private String adminPw;

    @NotBlank(message = "동아리 호수는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @ValidClubRoomNumber(groups = ValidationGroups.PatternGroup.class)
    private String clubRoomNumber;
}
