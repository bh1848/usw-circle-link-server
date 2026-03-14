package com.USWCicrcleLink.server.admin.admin.api;

import com.USWCicrcleLink.server.admin.admin.dto.AdminLoginRequest;
import com.USWCicrcleLink.server.admin.admin.dto.AdminLoginResponse;
import com.USWCicrcleLink.server.admin.admin.service.AdminLoginService;
import com.USWCicrcleLink.server.global.response.ApiResponse;
import com.USWCicrcleLink.server.global.validation.support.ValidationSequence;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminLoginController {
    private final AdminLoginService adminLoginService;

    /**
     * 로그인 (Admin)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> adminLogin(@RequestBody @Validated(ValidationSequence.class) AdminLoginRequest request, HttpServletResponse httpServletResponse){
        AdminLoginResponse adminLoginResponse = adminLoginService.adminLogin(request,httpServletResponse);
        return ResponseEntity.ok(new ApiResponse<>("운영팀 로그인 성공", adminLoginResponse));
    }
}
