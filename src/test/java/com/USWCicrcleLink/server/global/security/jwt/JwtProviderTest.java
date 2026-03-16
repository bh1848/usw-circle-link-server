package com.USWCicrcleLink.server.global.security.jwt;

import com.USWCicrcleLink.server.clubLeader.domain.Leader;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.TokenException;
import com.USWCicrcleLink.server.global.security.details.CustomLeaderDetails;
import com.USWCicrcleLink.server.global.security.details.CustomUserDetails;
import com.USWCicrcleLink.server.global.security.details.service.UserDetailsServiceManager;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.global.security.jwt.domain.TokenValidationResult;
import com.USWCicrcleLink.server.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class JwtProviderTest {

    private static final String TEST_SECRET_KEY = "test-secret-key-for-jwt-provider-unit-test-123456";
    private static final String USER_ACCOUNT = "user01";
    private static final String USER_PASSWORD = "encoded-password";
    private static final String USER_EMAIL = "user01@test.com";

    @Mock
    private UserDetailsServiceManager userDetailsServiceManager;

    @InjectMocks
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtProvider, "secretKeyString", TEST_SECRET_KEY);
        ReflectionTestUtils.invokeMethod(jwtProvider, "init");
    }

    @Nested
    class createAccessToken_테스트 {

        @Test
        void ADMIN_토큰을_생성할_수_있다() {
            UUID adminUUID = UUID.randomUUID();
            UserDetails adminDetails = mock(UserDetails.class);
            given(userDetailsServiceManager.loadUserByUuidAndRole(adminUUID, Role.ADMIN)).willReturn(adminDetails);

            String accessToken = jwtProvider.createAccessToken(adminUUID, Role.ADMIN);

            assertThat(accessToken).isNotBlank();
            assertThat(jwtProvider.validateAccessToken(accessToken)).isEqualTo(TokenValidationResult.VALID);
            then(userDetailsServiceManager).should().loadUserByUuidAndRole(adminUUID, Role.ADMIN);
        }

        @Test
        void USER_토큰을_생성할_수_있다() {
            UUID userUUID = UUID.randomUUID();
            UUID clubUUID = UUID.randomUUID();
            CustomUserDetails userDetails = new CustomUserDetails(createUser(userUUID, Role.USER), List.of(clubUUID));
            given(userDetailsServiceManager.loadUserByUuidAndRole(userUUID, Role.USER)).willReturn(userDetails);

            String accessToken = jwtProvider.createAccessToken(userUUID, Role.USER);

            assertThat(accessToken).isNotBlank();
            assertThat(jwtProvider.validateAccessToken(accessToken)).isEqualTo(TokenValidationResult.VALID);
            assertThat(jwtProvider.getRoleFromAccessToken(accessToken)).isEqualTo(Role.USER);
            then(userDetailsServiceManager).should().loadUserByUuidAndRole(userUUID, Role.USER);
        }

        @Test
        void LEADER_토큰을_생성할_수_있다() {
            UUID leaderUUID = UUID.randomUUID();
            UUID clubUUID = UUID.randomUUID();
            CustomLeaderDetails leaderDetails = new CustomLeaderDetails(createLeader(leaderUUID), clubUUID);
            given(userDetailsServiceManager.loadUserByUuidAndRole(leaderUUID, Role.LEADER)).willReturn(leaderDetails);

            String accessToken = jwtProvider.createAccessToken(leaderUUID, Role.LEADER);

            assertThat(accessToken).isNotBlank();
            assertThat(jwtProvider.validateAccessToken(accessToken)).isEqualTo(TokenValidationResult.VALID);
            assertThat(jwtProvider.getRoleFromAccessToken(accessToken)).isEqualTo(Role.LEADER);
            then(userDetailsServiceManager).should().loadUserByUuidAndRole(leaderUUID, Role.LEADER);
        }
    }

    @Nested
    class validateAccessToken_테스트 {

        @Test
        void 유효한_토큰이면_VALID를_반환한다() {
            UUID userUUID = UUID.randomUUID();
            String accessToken = createToken(userUUID, Role.USER, new Date(System.currentTimeMillis() + 60_000L));

            TokenValidationResult result = jwtProvider.validateAccessToken(accessToken);

            assertThat(result).isEqualTo(TokenValidationResult.VALID);
        }

        @Test
        void 만료된_토큰이면_EXPIRED를_반환한다() {
            UUID userUUID = UUID.randomUUID();
            String expiredToken = createToken(userUUID, Role.USER, new Date(System.currentTimeMillis() - 60_000L));

            TokenValidationResult result = jwtProvider.validateAccessToken(expiredToken);

            assertThat(result).isEqualTo(TokenValidationResult.EXPIRED);
        }

        @Test
        void 변조된_토큰이면_INVALID를_반환한다() {
            UUID userUUID = UUID.randomUUID();
            String accessToken = createToken(userUUID, Role.USER, new Date(System.currentTimeMillis() + 60_000L));
            String tamperedToken = accessToken + "tampered";

            TokenValidationResult result = jwtProvider.validateAccessToken(tamperedToken);

            assertThat(result).isEqualTo(TokenValidationResult.INVALID);
        }

        @Test
        void 빈_문자열이면_INVALID를_반환한다() {
            TokenValidationResult result = jwtProvider.validateAccessToken("");

            assertThat(result).isEqualTo(TokenValidationResult.INVALID);
        }
    }

    @Nested
    class resolveAccessToken_테스트 {

        @Test
        void Authorization_헤더에_Bearer_토큰이_있으면_파싱해서_반환한다() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(JwtProvider.AUTHORIZATION_HEADER, JwtProvider.BEARER_PREFIX + "access-token");

            String resolvedToken = jwtProvider.resolveAccessToken(request);

            assertThat(resolvedToken).isEqualTo("access-token");
        }

        @Test
        void 헤더가_없으면_null을_반환한다() {
            HttpServletRequest request = new MockHttpServletRequest();

            String resolvedToken = jwtProvider.resolveAccessToken(request);

            assertThat(resolvedToken).isNull();
        }

        @Test
        void Bearer_접두사가_없으면_null을_반환한다() {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader(JwtProvider.AUTHORIZATION_HEADER, "Basic access-token");

            String resolvedToken = jwtProvider.resolveAccessToken(request);

            assertThat(resolvedToken).isNull();
        }
    }

    @Nested
    class getRoleFromAccessToken_테스트 {

        @Test
        void 유효한_토큰에서_Role을_정상_추출한다() {
            UUID userUUID = UUID.randomUUID();
            String accessToken = createToken(userUUID, Role.ADMIN, new Date(System.currentTimeMillis() + 60_000L));

            Role role = jwtProvider.getRoleFromAccessToken(accessToken);

            assertThat(role).isEqualTo(Role.ADMIN);
        }

        @Test
        void 유효하지_않은_role_claim을_가진_토큰이면_TokenException이_발생한다() {
            String invalidRoleToken = createTokenWithRoleClaim(UUID.randomUUID(), "NOT_A_ROLE", new Date(System.currentTimeMillis() + 60_000L));

            assertThatThrownBy(() -> jwtProvider.getRoleFromAccessToken(invalidRoleToken))
                    .isInstanceOf(TokenException.class)
                    .extracting(exception -> ((TokenException) exception).getExceptionType())
                    .isEqualTo(ExceptionType.INVALID_TOKEN);
        }
    }

    private User createUser(UUID userUUID, Role role) {
        return User.builder()
                .userUUID(userUUID)
                .userAccount(USER_ACCOUNT)
                .userPw(USER_PASSWORD)
                .email(USER_EMAIL)
                .userCreatedAt(LocalDateTime.of(2026, 3, 17, 12, 0))
                .userUpdatedAt(LocalDateTime.of(2026, 3, 17, 12, 0))
                .role(role)
                .build();
    }

    private Leader createLeader(UUID leaderUUID) {
        return Leader.builder()
                .leaderUUID(leaderUUID)
                .leaderAccount("leader01")
                .leaderPw(USER_PASSWORD)
                .role(Role.LEADER)
                .build();
    }

    private String createToken(UUID uuid, Role role, Date expiration) {
        return createTokenWithRoleClaim(uuid, role.name(), expiration);
    }

    private String createTokenWithRoleClaim(UUID uuid, String roleClaim, Date expiration) {
        Claims claims = Jwts.claims().setSubject(uuid.toString());
        claims.put("role", roleClaim);
        Key secretKey = Keys.hmacShaKeyFor(TEST_SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }
}
