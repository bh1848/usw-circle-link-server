package com.USWCicrcleLink.server.user.dto;

import com.USWCicrcleLink.server.global.validation.support.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerifyEmailResponse {

    @NotNull
    private UUID emailToken_uuid;

    @NotBlank(message = "이메일 필수 입력 값입니다.")
    @Size(min = 1, max = 30, message = "이메일은 1~30자 이내여야 합니다.",groups = ValidationGroups.SizeGroup.class )
    private String email;
}
