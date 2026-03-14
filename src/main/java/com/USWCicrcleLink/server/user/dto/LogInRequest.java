package com.USWCicrcleLink.server.user.dto;

import com.USWCicrcleLink.server.global.bucket4j.ClientIdentifier;
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
public class LogInRequest implements ClientIdentifier {

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

    private String fcmToken;

    @Override
    public String getClientId() {
        return this.account;
    }
}
