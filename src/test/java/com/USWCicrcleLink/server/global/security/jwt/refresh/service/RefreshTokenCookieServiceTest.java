package com.USWCicrcleLink.server.global.security.jwt.refresh.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RefreshTokenCookieServiceTest {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String ANOTHER_COOKIE_NAME = "anotherCookie";
    private static final long EXPIRATION_TIME_MILLIS = 60_000L;
    private static final String SET_COOKIE_HEADER = "Set-Cookie";
    private static final String EXPECTED_ADD_COOKIE_HEADER =
            "refreshToken=refresh-token; Path=/; HttpOnly; Max-Age=60; SameSite=Strict; Secure";
    private static final String EXPECTED_DELETE_COOKIE_HEADER =
            "refreshToken=; Path=/; HttpOnly; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; SameSite=Strict";

    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private RefreshTokenCookieService refreshTokenCookieService;

    @Nested
    class resolve_테스트 {

        @Test
        void 리프레시_토큰_쿠키가_있으면_값을_반환한다() {
            Cookie refreshTokenCookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, REFRESH_TOKEN);
            Cookie anotherCookie = new Cookie(ANOTHER_COOKIE_NAME, "value");
            given(request.getCookies()).willReturn(new Cookie[]{anotherCookie, refreshTokenCookie});

            String result = refreshTokenCookieService.resolve(request);

            assertThat(result).isEqualTo(REFRESH_TOKEN);
            then(request).should().getCookies();
        }

        @Test
        void 쿠키가_null이면_null을_반환한다() {
            given(request.getCookies()).willReturn(null);

            String result = refreshTokenCookieService.resolve(request);

            assertThat(result).isNull();
            then(request).should().getCookies();
        }

        @Test
        void 리프레시_토큰_쿠키가_없으면_null을_반환한다() {
            Cookie anotherCookie = new Cookie(ANOTHER_COOKIE_NAME, "value");
            given(request.getCookies()).willReturn(new Cookie[]{anotherCookie});

            String result = refreshTokenCookieService.resolve(request);

            assertThat(result).isNull();
            then(request).should().getCookies();
        }
    }

    @Nested
    class addRefreshTokenCookie_테스트 {

        @Test
        void 리프레시_토큰_쿠키를_Set_Cookie_헤더로_추가한다() {
            ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);

            refreshTokenCookieService.addRefreshTokenCookie(response, REFRESH_TOKEN, EXPIRATION_TIME_MILLIS);

            then(response).should().addHeader(eq(SET_COOKIE_HEADER), headerValueCaptor.capture());
            assertThat(headerValueCaptor.getValue()).isEqualTo(EXPECTED_ADD_COOKIE_HEADER);
        }
    }

    @Nested
    class deleteRefreshTokenCookie_테스트 {

        @Test
        void 리프레시_토큰_삭제용_Set_Cookie_헤더를_추가한다() {
            ArgumentCaptor<String> headerValueCaptor = ArgumentCaptor.forClass(String.class);

            refreshTokenCookieService.deleteRefreshTokenCookie(response);

            then(response).should().addHeader(eq(SET_COOKIE_HEADER), headerValueCaptor.capture());
            assertThat(headerValueCaptor.getValue()).isEqualTo(EXPECTED_DELETE_COOKIE_HEADER);
        }
    }
}
