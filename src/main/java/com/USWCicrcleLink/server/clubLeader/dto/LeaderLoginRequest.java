package com.USWCicrcleLink.server.clubLeader.dto;

import com.USWCicrcleLink.server.global.bucket4j.ClientIdentifier;
import com.USWCicrcleLink.server.global.validation.support.ValidationGroups;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LeaderLoginRequest implements ClientIdentifier {
    @NotBlank(message = "아이디는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    private String leaderAccount;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.", groups = ValidationGroups.NotBlankGroup.class)
    private String leaderPw;

    @Override
    public String getClientId() {
        return this.leaderAccount;
    }
}
