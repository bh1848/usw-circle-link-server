package com.USWCicrcleLink.server.global.security.jwt.refresh.service;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.TokenException;
import com.USWCicrcleLink.server.global.security.jwt.JwtProvider;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.global.security.jwt.dto.TokenDto;
import com.USWCicrcleLink.server.global.security.jwt.refresh.domain.RefreshTokenSession;
import com.USWCicrcleLink.server.global.security.jwt.refresh.store.RefreshTokenStore;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 604_800_000L;

    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final RefreshTokenCookieService refreshTokenCookieService;

    public String issueRefreshToken(UUID uuid, Role role, HttpServletResponse response) {
        invalidateByUser(uuid);

        String refreshToken = UUID.randomUUID().toString();
        RefreshTokenSession session = new RefreshTokenSession(refreshToken, uuid, role, REFRESH_TOKEN_EXPIRATION_TIME);
        refreshTokenStore.save(session);
        refreshTokenCookieService.addRefreshTokenCookie(response, refreshToken, REFRESH_TOKEN_EXPIRATION_TIME);

        log.debug("새로운 Refresh Token 발급 - UUID: {}, Role: {}", uuid, role);
        return refreshToken;
    }

    public RefreshTokenSession getValidatedSession(String refreshToken, HttpServletRequest request) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new TokenException(ExceptionType.INVALID_TOKEN);
        }

        RefreshTokenSession session = refreshTokenStore.findByRefreshToken(refreshToken)
                .orElseThrow(() -> {
                    log.warn("Refresh Token 검증 실패 - IP: {} | 요청 경로: {}", request.getRemoteAddr(), request.getRequestURI());
                    return new TokenException(ExceptionType.INVALID_TOKEN);
                });

        String storedRefreshToken = refreshTokenStore.findRefreshTokenByUser(session.uuid()).orElse(null);
        if (!refreshToken.equals(storedRefreshToken)) {
            log.warn("Refresh Token 검증 실패 - 사용자 매핑 불일치 - UUID: {}", session.uuid());
            throw new TokenException(ExceptionType.INVALID_TOKEN);
        }

        return session;
    }

    public RefreshTokenSession getValidatedSessionFromRequest(HttpServletRequest request) {
        String refreshToken = resolveRefreshToken(request);
        if (!StringUtils.hasText(refreshToken)) {
            throw new TokenException(ExceptionType.INVALID_TOKEN);
        }
        return getValidatedSession(refreshToken, request);
    }

    public TokenDto rotateTokens(HttpServletRequest request, HttpServletResponse response) {
        RefreshTokenSession session = getValidatedSessionFromRequest(request);
        invalidateByUser(session.uuid());

        String accessToken = jwtProvider.createAccessToken(session.uuid(), session.role(), response);
        String refreshToken = issueRefreshToken(session.uuid(), session.role(), response);
        return new TokenDto(accessToken, refreshToken);
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        return refreshTokenCookieService.resolveRefreshToken(request);
    }

    public void invalidateByUser(UUID uuid) {
        refreshTokenStore.deleteByUser(uuid);
    }

    public void invalidateByUserAndClearCookie(UUID uuid, HttpServletResponse response) {
        invalidateByUser(uuid);
        clearRefreshTokenCookie(response);
    }

    public void clearRefreshTokenCookie(HttpServletResponse response) {
        refreshTokenCookieService.deleteRefreshTokenCookie(response);
    }
}
