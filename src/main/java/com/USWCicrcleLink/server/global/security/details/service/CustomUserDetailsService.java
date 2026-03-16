package com.USWCicrcleLink.server.global.security.details.service;

import com.USWCicrcleLink.server.club.repository.ClubMembersRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.ProfileException;
import com.USWCicrcleLink.server.global.exception.errortype.UserException;
import com.USWCicrcleLink.server.global.security.details.CustomUserDetails;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import com.USWCicrcleLink.server.user.domain.User;
import com.USWCicrcleLink.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomUserDetailsService implements RoleBasedUserDetailsService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ClubMembersRepository clubMembersRepository;

    @Override
    public Role getSupportedRole() {
        return Role.USER;
    }

    @Override
    public UserDetails loadUserByUuid(UUID uuid) {
        User user = userRepository.findByUserUUID(uuid)
                .orElseThrow(() -> new UserException(ExceptionType.USER_NOT_EXISTS));

        Profile profile = profileRepository.findByUser_UserUUID(user.getUserUUID())
                .orElseThrow(() -> new ProfileException(ExceptionType.PROFILE_NOT_EXISTS));

        List<UUID> clubUUIDs = getUserClubUUIDs(profile.getProfileId());
        return new CustomUserDetails(user, clubUUIDs);
    }

    private List<UUID> getUserClubUUIDs(Long profileId) {
        return clubMembersRepository.findClubUUIDsByProfileId(profileId);
    }
}
