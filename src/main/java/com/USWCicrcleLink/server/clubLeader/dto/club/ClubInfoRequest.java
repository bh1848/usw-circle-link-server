package com.USWCicrcleLink.server.clubLeader.dto.club;

import com.USWCicrcleLink.server.global.validation.annotation.Sanitize;
import com.USWCicrcleLink.server.global.validation.annotation.ValidClubRoomNumber;
import com.USWCicrcleLink.server.global.validation.support.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ClubInfoRequest {


    @Sanitize
    @NotBlank(message = "회장 이름은 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 2, max = 30, message = "회장 이름은 2~30자 이내여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^[a-zA-Z가-힣]+$", message = "회장 이름은 영어 또는 한글만 입력 가능합니다.", groups = ValidationGroups.PatternGroup.class)
    private String leaderName;

    @NotBlank(message = "전화번호는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 11, max = 11, message = "전화번호는 11자여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^01[0-9]{9}$", message = "올바른 전화번호를 입력하세요.", groups = ValidationGroups.PatternGroup.class)
    private String leaderHp;

    @Pattern(
            regexp = "^(https?://)?(www\\.)?instagram\\.com/.+$|^$",
            message = "유효한 인스타그램 링크를 입력해주세요."
    )
    private String clubInsta;

    @NotBlank(message = "동아리방 호수는 필수 입력 값입니다.")
    @ValidClubRoomNumber(groups = ValidationGroups.PatternGroup.class)
    private String clubRoomNumber;

    @Sanitize
    @Size(max = 2, message = "해시태그는 2개까지 입력 가능합니다.")
    private List<@Size(min = 1, max = 6, message = "각 해시태그는 최대 6자까지 입력 가능합니다.", groups = ValidationGroups.SizeGroup.class) String> clubHashtag;

    @Sanitize
    @Size(max = 3, message = "카테고리는 3개까지 입력 가능합니다.")
    private List<@Size(min = 1, max = 20, message = "각 카테고리는 20자까지 입력 가능합니다.", groups = ValidationGroups.SizeGroup.class) String> clubCategoryName;
}
