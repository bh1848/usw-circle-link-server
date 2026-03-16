package com.USWCicrcleLink.server.global.security.jwt.refresh.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class RefreshTokenCookieService {
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN_COOKIE_TEMPLATE = REFRESH_TOKEN_COOKIE_NAME + "=%s; Path=/; HttpOnly; Max-Age=%d; SameSite=Strict; Secure";
    private static final String REFRESH_TOKEN_COOKIE_DELETE_VALUE = REFRESH_TOKEN_COOKIE_NAME + "=; Path=/; HttpOnly; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Strict";

    public String resolve(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {
            if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken, long expirationTimeMillis) {
        int maxAge = (int) (expirationTimeMillis / 1000);
        response.addHeader("Set-Cookie", String.format(REFRESH_TOKEN_COOKIE_TEMPLATE, refreshToken, maxAge));
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie", REFRESH_TOKEN_COOKIE_DELETE_VALUE);
    }
}
