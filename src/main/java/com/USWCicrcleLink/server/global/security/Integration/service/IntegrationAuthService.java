package com.USWCicrcleLink.server.global.security.Integration.service;

import com.USWCicrcleLink.server.global.exception.errortype.TokenException;
import com.USWCicrcleLink.server.global.security.jwt.JwtProvider;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.global.security.jwt.dto.TokenDto;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class IntegrationAuthService {
    private final JwtProvider jwtProvider;
    private final ProfileRepository profileRepository;

    /**
     * 로그아웃 (User, Admin & Leader 통합)
     */
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtProvider.resolveRefreshToken(request);

        if (refreshToken != null) {
            try {
                jwtProvider.validateRefreshToken(refreshToken, request);
                UUID userUUID = jwtProvider.getUUIDFromRefreshToken(refreshToken);

                // 유저라면 FCM 토큰 삭제 (푸시 알림 무효화)
                profileRepository.findByUser_UserUUID(userUUID).ifPresent(profile -> {
                    profile.updateFcmToken(null);
                    profileRepository.save(profile);
                    log.debug("User 로그아웃 - FCM 토큰 삭제 완료 - UUID: {}", userUUID);
                });

                jwtProvider.deleteRefreshToken(userUUID);
            } catch (TokenException ignored) {
            }
        }

        SecurityContextHolder.clearContext();
        jwtProvider.deleteRefreshTokenCookie(response);
        log.debug("클라이언트 쿠키에서 리프레시 토큰 삭제 완료");
        log.debug("로그아웃 완료");
    }

    /**
     * 토큰 갱신 (User, Admin & Leader 통합)
     */
    public TokenDto refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtProvider.resolveRefreshToken(request);

        if (refreshToken == null) {
            logout(request, response);
            return null;
        }

        try {
            jwtProvider.validateRefreshToken(refreshToken, request);
            UUID uuid = jwtProvider.getUUIDFromRefreshToken(refreshToken);
            Role role = jwtProvider.getRoleFromRefreshToken(refreshToken);

            jwtProvider.deleteRefreshToken(uuid);

            String newAccessToken = jwtProvider.createAccessToken(uuid, role, response);
            String newRefreshToken = jwtProvider.createRefreshToken(uuid, role, response);

            log.debug("토큰 갱신 성공 - UUID: {}", uuid);
            return new TokenDto(newAccessToken, newRefreshToken);
        } catch (TokenException e) {
            logout(request, response);
            return null;
        }
    }
}
