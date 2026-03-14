package com.USWCicrcleLink.server.clubLeader.dto.clubMembers;

import com.USWCicrcleLink.server.global.validation.support.ValidationGroups;
import com.USWCicrcleLink.server.profile.domain.MemberType;
import com.USWCicrcleLink.server.profile.domain.Profile;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClubMembersResponse {

    private UUID clubMemberUUID;

    @NotBlank(message = "이름은 필수 입력 값입니다.",groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 2, max = 30, message = "이름은 2~30자 이내여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^[a-zA-Z가-힣]+$", message = "이름은 영어 또는 한글만 입력 가능합니다.", groups = ValidationGroups.PatternGroup.class)
    private String userName;

    private String major;

    @NotBlank(message = "학번은 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 8, max = 8, message = "학번은 8자리 숫자여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^[0-9]{8}$", message = "학번은 숫자만 입력 가능합니다.", groups = ValidationGroups.PatternGroup.class)
    private String studentNumber;

    @NotBlank(message = "전화번호는 필수 입력 값입니다.",groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 11, max = 11, message = "전화번호는 11자여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^01[0-9]{9}$", message = "올바른 전화번호를 입력하세요.", groups = ValidationGroups.PatternGroup.class)
    private String userHp;

    private MemberType memberType;

    public ClubMembersResponse(UUID clubMemberUUID, Profile profile) {
        this.clubMemberUUID = clubMemberUUID;
        this.userName = profile.getUserName();
        this.major = profile.getMajor();
        this.studentNumber = profile.getStudentNumber();
        this.userHp = profile.getUserHp();
        this.memberType = profile.getMemberType();
    }
}
