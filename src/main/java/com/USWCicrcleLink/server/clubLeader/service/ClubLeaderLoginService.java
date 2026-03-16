package com.USWCicrcleLink.server.clubLeader.service;

import com.USWCicrcleLink.server.clubLeader.domain.Leader;
import com.USWCicrcleLink.server.clubLeader.dto.LeaderLoginRequest;
import com.USWCicrcleLink.server.clubLeader.dto.LeaderLoginResponse;
import com.USWCicrcleLink.server.clubLeader.repository.LeaderRepository;
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
public class ClubLeaderLoginService {

    private final LeaderRepository leaderRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    /**
     * 로그인 (Leader)
     */
    @RateLimite(action = "WEB_LOGIN")
    public LeaderLoginResponse leaderLogin(LeaderLoginRequest request, HttpServletResponse response) {
        Leader leader = leaderRepository.findByLeaderAccount(request.getLeaderAccount())
                .orElseThrow(() -> new UserException(ExceptionType.USER_AUTHENTICATION_FAILED));

        if (!passwordEncoder.matches(request.getLeaderPw(), leader.getLeaderPw())) {
            throw new UserException(ExceptionType.USER_AUTHENTICATION_FAILED);
        }

        UUID clubUUID = leaderRepository.findClubUUIDByLeaderUUID(leader.getLeaderUUID())
                .orElseThrow(() -> new UserException(ExceptionType.USER_NOT_EXISTS));

        UUID leaderUUID = leader.getLeaderUUID();
        String accessToken = jwtProvider.createAccessToken(leaderUUID, leader.getRole(), response);
        String refreshToken = jwtProvider.createRefreshToken(leaderUUID, leader.getRole(), response);

        log.debug("Leader 로그인 성공 - uuid: {}, 클럽 UUID: {}", leaderUUID, clubUUID);
        return new LeaderLoginResponse(accessToken, refreshToken, Role.LEADER, clubUUID, leader.isAgreedTerms());
    }
}
