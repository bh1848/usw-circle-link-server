package com.USWCicrcleLink.server.profile.service;

import com.USWCicrcleLink.server.clubApplication.repository.ClubApplicationRepository;
import com.USWCicrcleLink.server.club.repository.ClubMembersRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.ProfileException;
import com.USWCicrcleLink.server.global.exception.errortype.UserException;
import com.USWCicrcleLink.server.global.security.details.CustomUserDetails;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import com.USWCicrcleLink.server.profile.dto.ProfileRequest;
import com.USWCicrcleLink.server.profile.dto.ProfileResponse;
import com.USWCicrcleLink.server.user.domain.User;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final ClubApplicationRepository clubApplicationRepository;
    private final ClubMembersRepository clubMembersRepository;
    private final PasswordEncoder passwordEncoder;


    //프로필 업데이트
    public ProfileResponse updateProfile(ProfileRequest profileRequest) {

        if (!confirmPW(profileRequest.getUserPw())) {
            throw new UserException(ExceptionType.USER_PASSWORD_NOT_MATCH);
        }

        validateProfileRequest(profileRequest);

        Profile profile = getProfileByAuth();

        profile.updateProfile(profileRequest.getUserName(),
                profileRequest.getStudentNumber(),
                profileRequest.getMajor(),
                profileRequest.getUserHp()
        );

        Profile updatedProfile = profileRepository.save(profile);

        if (updatedProfile == null) {
            log.error("프로필 업데이트 실패 {}", profile.getProfileId());
            throw new ProfileException(ExceptionType.PROFILE_UPDATE_FAIL);
        }

        log.debug("프로필 수정 완료");
        return new ProfileResponse(profile);
    }

    private void validateProfileRequest(ProfileRequest profileRequest) {
        if (profileRequest.getUserName() == null || profileRequest.getUserName().trim().isEmpty() ||
                profileRequest.getStudentNumber() == null || profileRequest.getStudentNumber().trim().isEmpty() ||
                profileRequest.getUserHp() == null || profileRequest.getUserHp().trim().isEmpty() ||
                profileRequest.getMajor() == null || profileRequest.getMajor().trim().isEmpty()) {

            throw new ProfileException(ExceptionType.PROFILE_NOT_INPUT);
        }
    }


    private Profile getProfileByAuth() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.user();

        return profileRepository.findByUserUserId(user.getUserId())
                .orElseThrow(() -> {
                    log.error("존재하지 않는 프로필");
                    throw new ProfileException(ExceptionType.PROFILE_NOT_EXISTS);
                });
    }

    //프로필 조회
    public ProfileResponse getMyProfile() {
        Profile profile = getProfileByAuth();
        return new ProfileResponse(profile);
    }

    // 어세스토큰에서 유저정보 가져오기
    public User getUserByAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.user();
    }

    //현재 비밀번호 확인
    private boolean confirmPW(String userpw) {
        User user = getUserByAuth();
        return passwordEncoder.matches(userpw, user.getUserPw());
    }

    @Transactional
    public void deleteProfileByUserUUID(UUID userUUID) {
        Profile profile = profileRepository.findByUser_UserUUID(userUUID)
                .orElseThrow(() -> new UserException(ExceptionType.USER_NOT_EXISTS));

        // 프로필과 연관된 테이블 데이터 삭제
        clubApplicationRepository.deleteAllByProfile(profile);
        clubMembersRepository.deleteAllByProfile(profile);

        // 프로필 삭제
        profileRepository.delete(profile);

        log.debug("프로필 및 연관 데이터 삭제 완료: {}", userUUID);
    }


    // 프로필 중복 확인
    public void checkProfileDuplicated(String userName, String userStudentNumber,String userHp) {
        // 프로필 중복확인
        log.debug("프로필 중복 체크 요청 시작 - 이름: {}, 전화번호: {}, 학번: {}",
                userName, userHp, userStudentNumber);
        profileRepository.findByUserNameAndStudentNumberAndUserHp(userName,userStudentNumber,userHp)
                .ifPresent(profile -> {
                    throw new ProfileException(ExceptionType.PROFILE_ALREADY_EXISTS);
                });
        log.debug("프로필 중복 확인 완료- 중복없음");
    }

}
