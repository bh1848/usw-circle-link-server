package com.USWCicrcleLink.server.global.security.jwt.refresh.service;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.TokenException;
import com.USWCicrcleLink.server.global.security.jwt.JwtProvider;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.global.security.jwt.dto.AccessTokenResponse;
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

    public void issue(UUID uuid, Role role, HttpServletResponse response) {
        invalidate(uuid);

        String refreshToken = UUID.randomUUID().toString();
        RefreshTokenSession session = new RefreshTokenSession(refreshToken, uuid, role, REFRESH_TOKEN_EXPIRATION_TIME);
        refreshTokenStore.save(session);
        refreshTokenCookieService.addRefreshTokenCookie(response, refreshToken, REFRESH_TOKEN_EXPIRATION_TIME);

        log.debug("새로운 Refresh Token 발급 - UUID: {}, Role: {}", uuid, role);
    }

    public RefreshTokenSession validate(String refreshToken, HttpServletRequest request) {
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

    public RefreshTokenSession validateFromRequest(HttpServletRequest request) {
        String refreshToken = resolve(request);
        if (!StringUtils.hasText(refreshToken)) {
            throw new TokenException(ExceptionType.INVALID_TOKEN);
        }
        return validate(refreshToken, request);
    }

    public AccessTokenResponse rotate(HttpServletRequest request, HttpServletResponse response) {
        RefreshTokenSession session = validateFromRequest(request);
        String accessToken = jwtProvider.createAccessToken(session.uuid(), session.role());
        issue(session.uuid(), session.role(), response);
        return new AccessTokenResponse(accessToken);
    }

    public String resolve(HttpServletRequest request) {
        return refreshTokenCookieService.resolve(request);
    }

    public void invalidate(UUID uuid) {
        refreshTokenStore.deleteByUser(uuid);
    }

    public void invalidateAndClearCookie(UUID uuid, HttpServletResponse response) {
        invalidate(uuid);
        clearCookie(response);
    }

    public void clearCookie(HttpServletResponse response) {
        refreshTokenCookieService.deleteRefreshTokenCookie(response);
    }
}
