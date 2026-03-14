package com.USWCicrcleLink.server.user.dto;

import com.USWCicrcleLink.server.global.validation.support.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoDto {

    @NotBlank(message = "아이디는 필수 입력 값입니다.",groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 5, max = 20, message = "아이디는 5~20자 이내여야 합니다.",groups = ValidationGroups.SizeGroup.class )
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문 대소문자 및 숫자만 가능합니다.",groups = ValidationGroups.PatternGroup.class)
    private String userAccount;

    @NotBlank(message = "이메일 필수 입력 값입니다.")
    @Size(min = 1, max = 30, message = "이메일은 1~30자 이내여야 합니다.",groups = ValidationGroups.SizeGroup.class )
    private String email;
}
