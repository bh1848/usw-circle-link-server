package com.USWCicrcleLink.server.admin.admin.dto;

import com.USWCicrcleLink.server.global.security.jwt.domain.Role;

public record AdminLoginResponse(
        String accessToken,
        Role role
) {
}
