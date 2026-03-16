package com.USWCicrcleLink.server.global.security.Integration.service;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.TokenException;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.global.security.jwt.dto.AccessTokenResponse;
import com.USWCicrcleLink.server.global.security.jwt.refresh.domain.RefreshTokenSession;
import com.USWCicrcleLink.server.global.security.jwt.refresh.service.RefreshTokenService;
import com.USWCicrcleLink.server.profile.domain.MemberType;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class IntegrationAuthServiceTest {

    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String ACCESS_TOKEN = "access-token";

    @Mock private ProfileRepository profileRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private IntegrationAuthService integrationAuthService;

    private UUID userUUID;
    private RefreshTokenSession refreshTokenSession;
    private Profile profile;

    @BeforeEach
    void setUp() {
        userUUID = UUID.randomUUID();
        refreshTokenSession = new RefreshTokenSession(REFRESH_TOKEN, userUUID, Role.USER, 1000L);
        profile = Profile.builder()
                .profileId(1L)
                .userName("홍길동")
                .studentNumber("20200001")
                .userHp("01012345678")
                .major("컴퓨터공학")
                .profileCreatedAt(LocalDateTime.of(2026, 3, 17, 12, 0))
                .profileUpdatedAt(LocalDateTime.of(2026, 3, 17, 12, 0))
                .memberType(MemberType.REGULARMEMBER)
                .fcmToken("fcm-token")
                .build();
    }

    @Nested
    class logout_테스트 {

        @Test
        void 리프레시_토큰이_없어도_컨텍스트를_비우고_쿠키를_삭제한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                given(refreshTokenService.resolve(request)).willReturn(null);

                integrationAuthService.logout(request, response);

                mocked.verify(SecurityContextHolder::clearContext);
                then(refreshTokenService).should().resolve(request);
                then(refreshTokenService).should().clearCookie(response);
                then(refreshTokenService).shouldHaveNoMoreInteractions();
                then(profileRepository).shouldHaveNoInteractions();
            }
        }

        @Test
        void 유효한_리프레시_토큰이면_FCM_토큰을_삭제하고_로그아웃한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                given(refreshTokenService.resolve(request)).willReturn(REFRESH_TOKEN);
                given(refreshTokenService.validate(REFRESH_TOKEN, request)).willReturn(refreshTokenSession);
                given(profileRepository.findByUser_UserUUID(userUUID)).willReturn(Optional.of(profile));

                integrationAuthService.logout(request, response);

                assertThat(profile.getFcmToken()).isNull();
                mocked.verify(SecurityContextHolder::clearContext);
                then(refreshTokenService).should().resolve(request);
                then(refreshTokenService).should().validate(REFRESH_TOKEN, request);
                then(profileRepository).should().findByUser_UserUUID(userUUID);
                then(profileRepository).should().save(profile);
                then(refreshTokenService).should().invalidate(userUUID);
                then(refreshTokenService).should().clearCookie(response);
            }
        }

        @Test
        void 프로필이_없어도_리프레시_토큰은_무효화한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                given(refreshTokenService.resolve(request)).willReturn(REFRESH_TOKEN);
                given(refreshTokenService.validate(REFRESH_TOKEN, request)).willReturn(refreshTokenSession);
                given(profileRepository.findByUser_UserUUID(userUUID)).willReturn(Optional.empty());

                integrationAuthService.logout(request, response);

                mocked.verify(SecurityContextHolder::clearContext);
                then(profileRepository).should().findByUser_UserUUID(userUUID);
                then(profileRepository).shouldHaveNoMoreInteractions();
                then(refreshTokenService).should().invalidate(userUUID);
                then(refreshTokenService).should().clearCookie(response);
            }
        }

        @Test
        void 토큰_검증에_실패해도_컨텍스트를_비우고_쿠키를_삭제한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                given(refreshTokenService.resolve(request)).willReturn(REFRESH_TOKEN);
                given(refreshTokenService.validate(REFRESH_TOKEN, request))
                        .willThrow(new TokenException(ExceptionType.INVALID_TOKEN));

                integrationAuthService.logout(request, response);

                mocked.verify(SecurityContextHolder::clearContext);
                then(refreshTokenService).should().resolve(request);
                then(refreshTokenService).should().validate(REFRESH_TOKEN, request);
                then(refreshTokenService).should().clearCookie(response);
                then(profileRepository).shouldHaveNoInteractions();
            }
        }
    }

    @Nested
    class refreshToken_테스트 {

        @Test
        void 리프레시_토큰이_없으면_로그아웃하고_null을_반환한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                given(refreshTokenService.resolve(request)).willReturn((String) null, (String) null);

                AccessTokenResponse result = integrationAuthService.refreshToken(request, response);

                assertThat(result).isNull();
                mocked.verify(SecurityContextHolder::clearContext);
                then(refreshTokenService).should(times(2)).resolve(request);
                then(refreshTokenService).should().clearCookie(response);
                then(refreshTokenService).shouldHaveNoMoreInteractions();
            }
        }

        @Test
        void 리프레시_토큰_회전에_성공하면_새_액세스_토큰을_반환한다() {
            AccessTokenResponse accessTokenResponse = new AccessTokenResponse(ACCESS_TOKEN);
            given(refreshTokenService.resolve(request)).willReturn(REFRESH_TOKEN);
            given(refreshTokenService.rotate(request, response)).willReturn(accessTokenResponse);

            AccessTokenResponse result = integrationAuthService.refreshToken(request, response);

            assertThat(result).isEqualTo(accessTokenResponse);
            then(refreshTokenService).should().resolve(request);
            then(refreshTokenService).should().rotate(request, response);
            then(profileRepository).shouldHaveNoInteractions();
        }

        @Test
        void 리프레시_토큰_회전에_실패하면_로그아웃하고_null을_반환한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                given(refreshTokenService.resolve(request)).willReturn(REFRESH_TOKEN, REFRESH_TOKEN);
                given(refreshTokenService.rotate(request, response))
                        .willThrow(new TokenException(ExceptionType.INVALID_TOKEN));
                given(refreshTokenService.validate(REFRESH_TOKEN, request))
                        .willThrow(new TokenException(ExceptionType.INVALID_TOKEN));

                AccessTokenResponse result = integrationAuthService.refreshToken(request, response);

                assertThat(result).isNull();
                mocked.verify(SecurityContextHolder::clearContext);
                then(refreshTokenService).should().rotate(request, response);
                then(refreshTokenService).should().validate(REFRESH_TOKEN, request);
                then(refreshTokenService).should().clearCookie(response);
            }
        }
    }
}
