package com.USWCicrcleLink.server.global.security.Integration.api;

import com.USWCicrcleLink.server.global.response.ApiResponse;
import com.USWCicrcleLink.server.global.security.Integration.service.IntegrationAuthService;
import com.USWCicrcleLink.server.global.security.jwt.dto.AccessTokenResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/integration")
@RequiredArgsConstructor
@Slf4j
public class IntegrationAuthController {
    private final IntegrationAuthService integrationAuthService;

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request, HttpServletResponse response) {
        integrationAuthService.logout(request, response);
        return ResponseEntity.ok(new ApiResponse<>("로그아웃 성공"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AccessTokenResponse>> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        AccessTokenResponse tokenResponse = integrationAuthService.refreshToken(request, response);

        if (tokenResponse == null) {
            return ResponseEntity.status(401).body(new ApiResponse<>("리프레시 토큰이 유효하지 않습니다. 로그아웃됐습니다.", null));
        }
        return ResponseEntity.ok(new ApiResponse<>("새로운 엑세스 토큰이 발급됐습니다.", tokenResponse));
    }
}
