package com.USWCicrcleLink.server.admin.admin.service;

import com.USWCicrcleLink.server.admin.admin.domain.Admin;
import com.USWCicrcleLink.server.admin.admin.dto.AdminLoginRequest;
import com.USWCicrcleLink.server.admin.admin.dto.AdminLoginResponse;
import com.USWCicrcleLink.server.admin.admin.repository.AdminRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.UserException;
import com.USWCicrcleLink.server.global.security.jwt.JwtProvider;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.global.security.jwt.refresh.service.RefreshTokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminLoginServiceTest {

    private static final String ADMIN_ACCOUNT = "admin";
    private static final String ADMIN_PASSWORD = "Admin!123";
    private static final String ENCODED_ADMIN_PASSWORD = "encodedAdminPw";
    private static final String ACCESS_TOKEN = "access-token";

    @Mock private AdminRepository adminRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtProvider jwtProvider;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private AdminLoginService adminLoginService;

    private Admin admin;
    private UUID adminUUID;
    private AdminLoginRequest request;

    @BeforeEach
    void setUp() {
        adminUUID = UUID.randomUUID();
        admin = Admin.builder()
                .adminUUID(adminUUID)
                .adminAccount(ADMIN_ACCOUNT)
                .adminPw(ENCODED_ADMIN_PASSWORD)
                .adminName("관리자")
                .role(Role.ADMIN)
                .build();
        request = new AdminLoginRequest(ADMIN_ACCOUNT, ADMIN_PASSWORD);
    }

    @Nested
    class adminLogin_테스트 {

        @Test
        void 관리자_로그인에_성공하면_액세스_토큰과_ADMIN_역할을_반환한다() {
            given(adminRepository.findByAdminAccount(ADMIN_ACCOUNT)).willReturn(Optional.of(admin));
            given(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_ADMIN_PASSWORD)).willReturn(true);
            given(jwtProvider.createAccessToken(adminUUID, Role.ADMIN)).willReturn(ACCESS_TOKEN);

            AdminLoginResponse result = adminLoginService.adminLogin(request, response);

            assertThat(result.accessToken()).isEqualTo(ACCESS_TOKEN);
            assertThat(result.role()).isEqualTo(Role.ADMIN);
            then(adminRepository).should().findByAdminAccount(ADMIN_ACCOUNT);
            then(passwordEncoder).should().matches(ADMIN_PASSWORD, ENCODED_ADMIN_PASSWORD);
            then(jwtProvider).should().createAccessToken(adminUUID, Role.ADMIN);
            then(refreshTokenService).should().issue(adminUUID, Role.ADMIN, response);
        }

        @Test
        void 관리자_계정이_없으면_USER_AUTHENTICATION_FAILED_예외가_발생한다() {
            given(adminRepository.findByAdminAccount(ADMIN_ACCOUNT)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminLoginService.adminLogin(request, response))
                    .isInstanceOf(UserException.class)
                    .extracting(e -> ((UserException) e).getExceptionType())
                    .isEqualTo(ExceptionType.USER_AUTHENTICATION_FAILED);

            then(passwordEncoder).shouldHaveNoInteractions();
            then(jwtProvider).shouldHaveNoInteractions();
            then(refreshTokenService).shouldHaveNoInteractions();
        }

        @Test
        void 관리자_비밀번호가_일치하지_않으면_USER_AUTHENTICATION_FAILED_예외가_발생한다() {
            given(adminRepository.findByAdminAccount(ADMIN_ACCOUNT)).willReturn(Optional.of(admin));
            given(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_ADMIN_PASSWORD)).willReturn(false);

            assertThatThrownBy(() -> adminLoginService.adminLogin(request, response))
                    .isInstanceOf(UserException.class)
                    .extracting(e -> ((UserException) e).getExceptionType())
                    .isEqualTo(ExceptionType.USER_AUTHENTICATION_FAILED);

            then(jwtProvider).shouldHaveNoInteractions();
            then(refreshTokenService).shouldHaveNoInteractions();
        }
    }
}
