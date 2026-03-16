package com.USWCicrcleLink.server.admin.admin.service;

import com.USWCicrcleLink.server.admin.admin.domain.Admin;
import com.USWCicrcleLink.server.admin.admin.dto.AdminLoginRequest;
import com.USWCicrcleLink.server.admin.admin.dto.AdminLoginResponse;
import com.USWCicrcleLink.server.admin.admin.repository.AdminRepository;
import com.USWCicrcleLink.server.global.bucket4j.RateLimite;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.UserException;
import com.USWCicrcleLink.server.global.security.jwt.JwtProvider;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminLoginService {
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 로그인 (Admin)
     */
    @RateLimite(action = "WEB_LOGIN")
    public AdminLoginResponse adminLogin(AdminLoginRequest request, HttpServletResponse response) {
        Admin admin = adminRepository.findByAdminAccount(request.getAdminAccount())
                .orElseThrow(() -> new UserException(ExceptionType.USER_AUTHENTICATION_FAILED));

        if (!passwordEncoder.matches(request.getAdminPw(), admin.getAdminPw())) {
            throw new UserException(ExceptionType.USER_AUTHENTICATION_FAILED);
        }

        UUID adminUUID = admin.getAdminUUID();
        String accessToken = jwtProvider.createAccessToken(adminUUID, admin.getRole(), response);
        String refreshToken = jwtProvider.createRefreshToken(adminUUID, admin.getRole(), response);

        log.debug("Admin 로그인 성공 - uuid: {}", adminUUID);
        return new AdminLoginResponse(accessToken, refreshToken, Role.ADMIN);
    }
}
