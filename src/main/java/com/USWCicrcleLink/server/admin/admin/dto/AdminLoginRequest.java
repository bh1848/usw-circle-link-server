package com.USWCicrcleLink.server.admin.admin.dto;

import com.USWCicrcleLink.server.global.bucket4j.ClientIdentifier;
import com.USWCicrcleLink.server.global.validation.support.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AdminLoginRequest implements ClientIdentifier {
    @NotBlank(message = "아이디는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    private String adminAccount;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    private String adminPw;

    @Override
    public String getClientId() {
        return this.adminAccount;
    }
}
