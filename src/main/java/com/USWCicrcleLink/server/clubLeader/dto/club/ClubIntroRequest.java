package com.USWCicrcleLink.server.clubLeader.dto.club;

import com.USWCicrcleLink.server.club.domain.RecruitmentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ClubIntroRequest {

    @Size(max = 3000, message = "소개글은 최대 3000자까지 입력 가능합니다.")
    private String clubIntro;

    @NotNull(message = "모집 상태를 설정해주세요.")
    private RecruitmentStatus recruitmentStatus;

    @Size(max = 3000, message = "모집글은 최대 3000자까지 입력 가능합니다.")
    private String clubRecruitment;

    @Pattern(
            regexp = "^(https://[a-zA-Z0-9._-]+(?:\\.[a-zA-Z]{2,})+.*)?$",
            message = "유효한 HTTPS 링크를 입력해주세요."
    )
    private String googleFormUrl;

    private List<Integer> orders;

    private List<Integer> deletedOrders;

}
