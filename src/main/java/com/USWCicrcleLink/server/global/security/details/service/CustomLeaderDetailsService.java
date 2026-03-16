package com.USWCicrcleLink.server.global.security.details.service;

import com.USWCicrcleLink.server.clubLeader.domain.Leader;
import com.USWCicrcleLink.server.clubLeader.repository.LeaderRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.UserException;
import com.USWCicrcleLink.server.global.security.details.CustomLeaderDetails;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomLeaderDetailsService implements RoleBasedUserDetailsService {

    private final LeaderRepository leaderRepository;

    @Override
    public Role getSupportedRole() {
        return Role.LEADER;
    }

    @Override
    public UserDetails loadUserByUuid(UUID uuid) {
        Leader leader = leaderRepository.findByLeaderUUID(uuid)
                .orElseThrow(() -> new UserException(ExceptionType.USER_NOT_EXISTS));

        UUID clubUUID = leaderRepository.findClubUUIDByLeaderUUID(leader.getLeaderUUID())
                .orElseThrow(() -> new UserException(ExceptionType.USER_NOT_EXISTS));

        return new CustomLeaderDetails(leader, clubUUID);
    }
}
