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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String STORED_REFRESH_TOKEN = "stored-refresh-token";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String REQUEST_URI = "/api/auth/refresh";
    private static final String REMOTE_ADDR = "127.0.0.1";
    private static final long REFRESH_TOKEN_EXPIRATION_TIME = 604_800_000L;

    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenStore refreshTokenStore;
    @Mock private RefreshTokenCookieService refreshTokenCookieService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    @Captor
    private ArgumentCaptor<RefreshTokenSession> refreshTokenSessionCaptor;

    private UUID userUUID;
    private RefreshTokenSession refreshTokenSession;

    @BeforeEach
    void setUp() {
        userUUID = UUID.randomUUID();
        refreshTokenSession = new RefreshTokenSession(REFRESH_TOKEN, userUUID, Role.USER, REFRESH_TOKEN_EXPIRATION_TIME);
    }

    @Nested
    class issue_테스트 {

        @Test
        void 리프레시_토큰을_저장하고_쿠키를_발급한다() {
            refreshTokenService.issue(userUUID, Role.USER, response);

            then(refreshTokenStore).should().deleteByUser(userUUID);
            then(refreshTokenStore).should().save(refreshTokenSessionCaptor.capture());
            then(refreshTokenCookieService).should().addRefreshTokenCookie(
                    response,
                    refreshTokenSessionCaptor.getValue().refreshToken(),
                    REFRESH_TOKEN_EXPIRATION_TIME
            );
            assertThat(refreshTokenSessionCaptor.getValue().uuid()).isEqualTo(userUUID);
            assertThat(refreshTokenSessionCaptor.getValue().role()).isEqualTo(Role.USER);
            assertThat(refreshTokenSessionCaptor.getValue().expirationTime()).isEqualTo(REFRESH_TOKEN_EXPIRATION_TIME);
            assertThat(refreshTokenSessionCaptor.getValue().refreshToken()).isNotBlank();
        }
    }

    @Nested
    class validate_테스트 {

        @Test
        void 리프레시_토큰이_비어있으면_INVALID_TOKEN_예외가_발생한다() {
            assertThatThrownBy(() -> refreshTokenService.validate("", request))
                    .isInstanceOf(TokenException.class)
                    .extracting(e -> ((TokenException) e).getExceptionType())
                    .isEqualTo(ExceptionType.INVALID_TOKEN);
        }

        @Test
        void 저장된_세션이_없으면_INVALID_TOKEN_예외가_발생한다() {
            given(request.getRemoteAddr()).willReturn(REMOTE_ADDR);
            given(request.getRequestURI()).willReturn(REQUEST_URI);
            given(refreshTokenStore.findByRefreshToken(REFRESH_TOKEN)).willReturn(Optional.empty());

            assertThatThrownBy(() -> refreshTokenService.validate(REFRESH_TOKEN, request))
                    .isInstanceOf(TokenException.class)
                    .extracting(e -> ((TokenException) e).getExceptionType())
                    .isEqualTo(ExceptionType.INVALID_TOKEN);

            then(refreshTokenStore).should().findByRefreshToken(REFRESH_TOKEN);
        }

        @Test
        void 사용자에_매핑된_토큰이_다르면_INVALID_TOKEN_예외가_발생한다() {
            given(refreshTokenStore.findByRefreshToken(REFRESH_TOKEN)).willReturn(Optional.of(refreshTokenSession));
            given(refreshTokenStore.findRefreshTokenByUser(userUUID)).willReturn(Optional.of(STORED_REFRESH_TOKEN));

            assertThatThrownBy(() -> refreshTokenService.validate(REFRESH_TOKEN, request))
                    .isInstanceOf(TokenException.class)
                    .extracting(e -> ((TokenException) e).getExceptionType())
                    .isEqualTo(ExceptionType.INVALID_TOKEN);

            then(refreshTokenStore).should().findByRefreshToken(REFRESH_TOKEN);
            then(refreshTokenStore).should().findRefreshTokenByUser(userUUID);
        }

        @Test
        void 저장된_세션과_사용자_매핑이_일치하면_세션을_반환한다() {
            given(refreshTokenStore.findByRefreshToken(REFRESH_TOKEN)).willReturn(Optional.of(refreshTokenSession));
            given(refreshTokenStore.findRefreshTokenByUser(userUUID)).willReturn(Optional.of(REFRESH_TOKEN));

            RefreshTokenSession result = refreshTokenService.validate(REFRESH_TOKEN, request);

            assertThat(result).isEqualTo(refreshTokenSession);
            then(refreshTokenStore).should().findByRefreshToken(REFRESH_TOKEN);
            then(refreshTokenStore).should().findRefreshTokenByUser(userUUID);
        }
    }

    @Nested
    class validateFromRequest_테스트 {

        @Test
        void 요청에서_리프레시_토큰을_찾지_못하면_INVALID_TOKEN_예외가_발생한다() {
            given(refreshTokenCookieService.resolve(request)).willReturn(null);

            assertThatThrownBy(() -> refreshTokenService.validateFromRequest(request))
                    .isInstanceOf(TokenException.class)
                    .extracting(e -> ((TokenException) e).getExceptionType())
                    .isEqualTo(ExceptionType.INVALID_TOKEN);

            then(refreshTokenCookieService).should().resolve(request);
        }

        @Test
        void 요청의_리프레시_토큰을_정상_검증해_세션을_반환한다() {
            given(refreshTokenCookieService.resolve(request)).willReturn(REFRESH_TOKEN);
            given(refreshTokenStore.findByRefreshToken(REFRESH_TOKEN)).willReturn(Optional.of(refreshTokenSession));
            given(refreshTokenStore.findRefreshTokenByUser(userUUID)).willReturn(Optional.of(REFRESH_TOKEN));

            RefreshTokenSession result = refreshTokenService.validateFromRequest(request);

            assertThat(result).isEqualTo(refreshTokenSession);
            then(refreshTokenCookieService).should().resolve(request);
            then(refreshTokenStore).should().findByRefreshToken(REFRESH_TOKEN);
            then(refreshTokenStore).should().findRefreshTokenByUser(userUUID);
        }
    }

    @Nested
    class rotate_테스트 {

        @Test
        void 리프레시_토큰이_유효하면_새_액세스_토큰을_반환하고_리프레시_토큰을_재발급한다() {
            given(refreshTokenCookieService.resolve(request)).willReturn(REFRESH_TOKEN);
            given(refreshTokenStore.findByRefreshToken(REFRESH_TOKEN)).willReturn(Optional.of(refreshTokenSession));
            given(refreshTokenStore.findRefreshTokenByUser(userUUID)).willReturn(Optional.of(REFRESH_TOKEN));
            given(jwtProvider.createAccessToken(userUUID, Role.USER)).willReturn(ACCESS_TOKEN);

            AccessTokenResponse result = refreshTokenService.rotate(request, response);

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            then(refreshTokenCookieService).should().resolve(request);
            then(refreshTokenStore).should().findByRefreshToken(REFRESH_TOKEN);
            then(refreshTokenStore).should().findRefreshTokenByUser(userUUID);
            then(jwtProvider).should().createAccessToken(userUUID, Role.USER);
            then(refreshTokenStore).should().deleteByUser(userUUID);
            then(refreshTokenStore).should().save(refreshTokenSessionCaptor.capture());
            then(refreshTokenCookieService).should().addRefreshTokenCookie(
                    response,
                    refreshTokenSessionCaptor.getValue().refreshToken(),
                    REFRESH_TOKEN_EXPIRATION_TIME
            );
            assertThat(refreshTokenSessionCaptor.getValue().uuid()).isEqualTo(userUUID);
            assertThat(refreshTokenSessionCaptor.getValue().role()).isEqualTo(Role.USER);
        }

        @Test
        void 요청의_리프레시_토큰이_없으면_INVALID_TOKEN_예외가_발생한다() {
            given(refreshTokenCookieService.resolve(request)).willReturn(null);

            assertThatThrownBy(() -> refreshTokenService.rotate(request, response))
                    .isInstanceOf(TokenException.class)
                    .extracting(e -> ((TokenException) e).getExceptionType())
                    .isEqualTo(ExceptionType.INVALID_TOKEN);
        }
    }

    @Nested
    class resolve_테스트 {

        @Test
        void 쿠키_서비스의_리졸브_결과를_반환한다() {
            given(refreshTokenCookieService.resolve(request)).willReturn(REFRESH_TOKEN);

            String result = refreshTokenService.resolve(request);

            assertThat(result).isEqualTo(REFRESH_TOKEN);
            then(refreshTokenCookieService).should().resolve(request);
        }
    }

    @Nested
    class invalidate_테스트 {

        @Test
        void 사용자에_매핑된_리프레시_토큰을_삭제한다() {
            refreshTokenService.invalidate(userUUID);

            then(refreshTokenStore).should().deleteByUser(userUUID);
        }
    }

    @Nested
    class invalidateAndClearCookie_테스트 {

        @Test
        void 사용자_토큰을_삭제하고_쿠키를_삭제한다() {
            refreshTokenService.invalidateAndClearCookie(userUUID, response);

            then(refreshTokenStore).should().deleteByUser(userUUID);
            then(refreshTokenCookieService).should().deleteRefreshTokenCookie(response);
        }
    }

    @Nested
    class clearCookie_테스트 {

        @Test
        void 리프레시_토큰_쿠키를_삭제한다() {
            refreshTokenService.clearCookie(response);

            then(refreshTokenCookieService).should().deleteRefreshTokenCookie(response);
        }
    }
}
