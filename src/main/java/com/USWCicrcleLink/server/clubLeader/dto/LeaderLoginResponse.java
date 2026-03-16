package com.USWCicrcleLink.server.clubLeader.dto;

import com.USWCicrcleLink.server.global.security.jwt.domain.Role;

import java.util.UUID;

public record LeaderLoginResponse(
        String accessToken,
        Role role,
        UUID clubUUID,
        Boolean isAgreedTerms
) {
}
