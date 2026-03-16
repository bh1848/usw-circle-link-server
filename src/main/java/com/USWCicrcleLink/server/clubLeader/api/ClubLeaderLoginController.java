package com.USWCicrcleLink.server.clubLeader.api;

import com.USWCicrcleLink.server.clubLeader.dto.LeaderLoginRequest;
import com.USWCicrcleLink.server.clubLeader.dto.LeaderLoginResponse;
import com.USWCicrcleLink.server.clubLeader.service.ClubLeaderLoginService;
import com.USWCicrcleLink.server.global.response.ApiResponse;
import com.USWCicrcleLink.server.global.validation.support.ValidationSequence;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/club-leader")
@Slf4j
public class ClubLeaderLoginController {

    private final ClubLeaderLoginService clubLeaderLoginService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LeaderLoginResponse>> leaderLogin(@RequestBody @Validated(ValidationSequence.class) LeaderLoginRequest request, HttpServletResponse response) {
        LeaderLoginResponse leaderLoginResponse = clubLeaderLoginService.leaderLogin(request, response);
        ApiResponse<LeaderLoginResponse> apiResponse = new ApiResponse<>("동아리 회장 로그인 성공", leaderLoginResponse);
        return ResponseEntity.ok(apiResponse);
    }
}
