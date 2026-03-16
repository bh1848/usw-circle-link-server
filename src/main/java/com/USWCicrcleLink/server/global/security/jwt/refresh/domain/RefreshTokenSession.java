package com.USWCicrcleLink.server.global.security.jwt.refresh.domain;

import com.USWCicrcleLink.server.global.security.jwt.domain.Role;

import java.util.UUID;

public record RefreshTokenSession(
        String refreshToken,
        UUID uuid,
        Role role,
        long expirationTime
) {
}
