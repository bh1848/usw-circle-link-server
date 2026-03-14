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
public class SignUpRequest {

    @NotBlank(message = "아이디는 필수 입력 값입니다.",groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 5, max = 20, message = "아이디는 5~20자 이내여야 합니다.",groups = ValidationGroups.SizeGroup.class )
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문 대소문자 및 숫자만 가능합니다.",groups = ValidationGroups.PatternGroup.class)
    private String account;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.",groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 8, max = 20, message = "비밀번호는 8~20자 이내여야 합니다.",groups = ValidationGroups.SizeGroup.class)
    @Pattern(
            regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])(?!.*\\s).*$",
            message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 하며 공백을 포함할 수 없습니다.",
            groups = ValidationGroups.PatternGroup.class
    )
    private String password;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.",groups = ValidationGroups.NotBlankGroup.class)
    private String confirmPassword;

    @NotBlank(message = "이름은 필수 입력 값입니다.",groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 2, max = 30, message = "이름은 2~30자 이내여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^[a-zA-Z가-힣]+$", message = "이름은 영어 또는 한글만 입력 가능합니다.", groups = ValidationGroups.PatternGroup.class)
    private String userName;

    @NotBlank(message = "전화번호는 필수 입력 값입니다.",groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 11, max = 11, message = "전화번호는 11자여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^01[0-9]{9}$", message = "올바른 전화번호를 입력하세요.", groups = ValidationGroups.PatternGroup.class)
    private String telephone;

    @NotBlank(message = "학번은 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 8, max = 8, message = "학번은 8자리 숫자여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^[0-9]{8}$", message = "학번은 숫자만 입력 가능합니다.", groups = ValidationGroups.PatternGroup.class)
    private String studentNumber;

    @NotBlank(message = "학과는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    @Size(min = 1, max = 20, message = "학과는 1~20자 이내여야 합니다.", groups = ValidationGroups.SizeGroup.class)
    @Pattern(regexp = "^[가-힣a-zA-Z]+$", message = "학과는 한글 또는 영어만 입력 가능합니다.", groups = ValidationGroups.PatternGroup.class)
    private String major;

}
