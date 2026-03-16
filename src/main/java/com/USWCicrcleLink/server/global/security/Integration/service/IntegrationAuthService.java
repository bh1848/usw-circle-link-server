package com.USWCicrcleLink.server.global.security.Integration.service;

import com.USWCicrcleLink.server.global.exception.errortype.TokenException;
import com.USWCicrcleLink.server.global.security.jwt.refresh.domain.RefreshTokenSession;
import com.USWCicrcleLink.server.global.security.jwt.refresh.service.RefreshTokenService;
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
    private final ProfileRepository profileRepository;
    private final RefreshTokenService refreshTokenService;

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = refreshTokenService.resolveRefreshToken(request);
        if (refreshToken != null) {
            try {
                RefreshTokenSession session = refreshTokenService.getValidatedSession(refreshToken, request);
                UUID userUUID = session.uuid();

                profileRepository.findByUser_UserUUID(userUUID).ifPresent(profile -> {
                    profile.updateFcmToken(null);
                    profileRepository.save(profile);
                    log.debug("User 로그아웃 - FCM 토큰 삭제 완료 - UUID: {}", userUUID);
                });

                refreshTokenService.invalidateByUser(userUUID);
            } catch (TokenException ignored) {
            }
        }

        SecurityContextHolder.clearContext();
        refreshTokenService.clearRefreshTokenCookie(response);
        log.debug("로그아웃 완료");
    }

    public TokenDto refreshToken(HttpServletRequest request, HttpServletResponse response) {
        if (refreshTokenService.resolveRefreshToken(request) == null) {
            logout(request, response);
            return null;
        }

        try {
            TokenDto tokenDto = refreshTokenService.rotateTokens(request, response);
            log.debug("토큰 갱신 성공");
            return tokenDto;
        } catch (TokenException e) {
            logout(request, response);
            return null;
        }
    }
}
