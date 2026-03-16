package com.USWCicrcleLink.server.clubApplication.service;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.repository.ClubMembersRepository;
import com.USWCicrcleLink.server.club.repository.ClubRepository;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import com.USWCicrcleLink.server.clubApplication.repository.ClubApplicationRepository;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntro;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.ClubApplicationException;
import com.USWCicrcleLink.server.global.exception.errortype.ClubException;
import com.USWCicrcleLink.server.global.security.details.CustomUserDetails;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import com.USWCicrcleLink.server.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClubApplicationServiceTest {

    @Mock private ClubApplicationRepository clubApplicationRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private ClubIntroRepository clubIntroRepository;
    @Mock private ClubMembersRepository clubMembersRepository;

    @InjectMocks
    private ClubApplicationService clubApplicationService;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    private UUID clubUUID;
    private UUID userUUID;
    private User user;
    private Profile profile;
    private Club club;

    @BeforeEach
    void setUp() {
        clubUUID = UUID.randomUUID();
        userUUID = UUID.randomUUID();

        user = mock(User.class);
        profile = mock(Profile.class);
        club = mock(Club.class);
    }

    // ===== 공통: SecurityContextHolder 셋업 =====
    private void mockAuthentication() {
        given(user.getUserUUID()).willReturn(userUUID);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(new CustomUserDetails(user, List.of()));
        given(profileRepository.findByUser_UserUUID(userUUID)).willReturn(Optional.of(profile));
    }

    // ===== checkIfCanApply =====

    @Test
    @DisplayName("이미 지원한 경우 ALREADY_APPLIED 예외 발생")
    void checkIfCanApply_alreadyApplied_throwsAlreadyApplied() {
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthentication();

            given(clubApplicationRepository.existsByProfileAndClubUUID(profile, clubUUID)).willReturn(true);

            assertThatThrownBy(() -> clubApplicationService.checkIfCanApply(clubUUID))
                    .isInstanceOf(ClubApplicationException.class)
                    .extracting(e -> ((ClubApplicationException) e).getExceptionType())
                    .isEqualTo(ExceptionType.ALREADY_APPLIED);
        }
    }

    @Test
    @DisplayName("이미 동아리 회원인 경우 ALREADY_MEMBER 예외 발생")
    void checkIfCanApply_alreadyMember_throwsAlreadyMember() {
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthentication();

            given(clubApplicationRepository.existsByProfileAndClubUUID(profile, clubUUID)).willReturn(false);
            given(clubMembersRepository.existsByProfileAndClubUUID(profile, clubUUID)).willReturn(true);

            assertThatThrownBy(() -> clubApplicationService.checkIfCanApply(clubUUID))
                    .isInstanceOf(ClubApplicationException.class)
                    .extracting(e -> ((ClubApplicationException) e).getExceptionType())
                    .isEqualTo(ExceptionType.ALREADY_MEMBER);
        }
    }

    @Test
    @DisplayName("전화번호 중복인 경우 PHONE_NUMBER_ALREADY_REGISTERED 예외 발생")
    void checkIfCanApply_phoneNumberConflict_throwsPhoneNumberAlreadyRegistered() {
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthentication();

            given(clubApplicationRepository.existsByProfileAndClubUUID(profile, clubUUID)).willReturn(false);
            given(clubMembersRepository.existsByProfileAndClubUUID(profile, clubUUID)).willReturn(false);

            Profile existingMember = mock(Profile.class);
            given(profile.getUserHp()).willReturn("01012345678");
            given(existingMember.getUserHp()).willReturn("01012345678");
            given(clubMembersRepository.findProfilesByClubUUID(clubUUID)).willReturn(List.of(existingMember));

            assertThatThrownBy(() -> clubApplicationService.checkIfCanApply(clubUUID))
                    .isInstanceOf(ClubApplicationException.class)
                    .extracting(e -> ((ClubApplicationException) e).getExceptionType())
                    .isEqualTo(ExceptionType.PHONE_NUMBER_ALREADY_REGISTERED);
        }
    }

    @Test
    @DisplayName("학번 중복인 경우 STUDENT_NUMBER_ALREADY_REGISTERED 예외 발생")
    void checkIfCanApply_studentNumberConflict_throwsStudentNumberAlreadyRegistered() {
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthentication();

            given(clubApplicationRepository.existsByProfileAndClubUUID(profile, clubUUID)).willReturn(false);
            given(clubMembersRepository.existsByProfileAndClubUUID(profile, clubUUID)).willReturn(false);

            Profile existingMember = mock(Profile.class);
            given(profile.getUserHp()).willReturn("01011111111");
            given(existingMember.getUserHp()).willReturn("01099999999");
            given(profile.getStudentNumber()).willReturn("20200001");
            given(existingMember.getStudentNumber()).willReturn("20200001");
            given(clubMembersRepository.findProfilesByClubUUID(clubUUID)).willReturn(List.of(existingMember));

            assertThatThrownBy(() -> clubApplicationService.checkIfCanApply(clubUUID))
                    .isInstanceOf(ClubApplicationException.class)
                    .extracting(e -> ((ClubApplicationException) e).getExceptionType())
                    .isEqualTo(ExceptionType.STUDENT_NUMBER_ALREADY_REGISTERED);
        }
    }

    @Test
    @DisplayName("모든 조건 통과 시 예외 없이 정상 처리")
    void checkIfCanApply_allChecksPassed_noException() {
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthentication();

            given(clubApplicationRepository.existsByProfileAndClubUUID(profile, clubUUID)).willReturn(false);
            given(clubMembersRepository.existsByProfileAndClubUUID(profile, clubUUID)).willReturn(false);

            Profile existingMember = mock(Profile.class);
            given(profile.getUserHp()).willReturn("01011111111");
            given(existingMember.getUserHp()).willReturn("01099999999");
            given(profile.getStudentNumber()).willReturn("20200001");
            given(existingMember.getStudentNumber()).willReturn("20200002");
            given(clubMembersRepository.findProfilesByClubUUID(clubUUID)).willReturn(List.of(existingMember));

            // 예외 없이 통과
            clubApplicationService.checkIfCanApply(clubUUID);
        }
    }

    // ===== getGoogleFormUrlByClubUUID =====

    @Test
    @DisplayName("동아리 소개글이 없으면 CLUB_INTRO_NOT_EXISTS 예외 발생")
    void getGoogleFormUrl_clubIntroNotFound_throwsClubIntroNotExists() {
        given(clubIntroRepository.findByClubUUID(clubUUID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> clubApplicationService.getGoogleFormUrlByClubUUID(clubUUID))
                .isInstanceOf(ClubException.class)
                .extracting(e -> ((ClubException) e).getExceptionType())
                .isEqualTo(ExceptionType.CLUB_INTRO_NOT_EXISTS);
    }

    @Test
    @DisplayName("구글 폼 URL이 null이면 GOOGLE_FORM_URL_NOT_EXISTS 예외 발생")
    void getGoogleFormUrl_urlIsNull_throwsGoogleFormUrlNotExists() {
        ClubIntro clubIntro = mock(ClubIntro.class);
        given(clubIntroRepository.findByClubUUID(clubUUID)).willReturn(Optional.of(clubIntro));
        given(clubIntro.getGoogleFormUrl()).willReturn(null);

        assertThatThrownBy(() -> clubApplicationService.getGoogleFormUrlByClubUUID(clubUUID))
                .isInstanceOf(ClubException.class)
                .extracting(e -> ((ClubException) e).getExceptionType())
                .isEqualTo(ExceptionType.GOOGLE_FORM_URL_NOT_EXISTS);
    }

    @Test
    @DisplayName("구글 폼 URL이 빈 문자열이면 GOOGLE_FORM_URL_NOT_EXISTS 예외 발생")
    void getGoogleFormUrl_urlIsEmpty_throwsGoogleFormUrlNotExists() {
        ClubIntro clubIntro = mock(ClubIntro.class);
        given(clubIntroRepository.findByClubUUID(clubUUID)).willReturn(Optional.of(clubIntro));
        given(clubIntro.getGoogleFormUrl()).willReturn("");

        assertThatThrownBy(() -> clubApplicationService.getGoogleFormUrlByClubUUID(clubUUID))
                .isInstanceOf(ClubException.class)
                .extracting(e -> ((ClubException) e).getExceptionType())
                .isEqualTo(ExceptionType.GOOGLE_FORM_URL_NOT_EXISTS);
    }

    @Test
    @DisplayName("구글 폼 URL 정상 반환")
    void getGoogleFormUrl_success_returnsUrl() {
        String expectedUrl = "https://forms.google.com/test";
        ClubIntro clubIntro = mock(ClubIntro.class);
        given(clubIntroRepository.findByClubUUID(clubUUID)).willReturn(Optional.of(clubIntro));
        given(clubIntro.getGoogleFormUrl()).willReturn(expectedUrl);

        String result = clubApplicationService.getGoogleFormUrlByClubUUID(clubUUID);

        assertThat(result).isEqualTo(expectedUrl);
    }

    // ===== submitClubApplication =====

    @Test
    @DisplayName("동아리가 존재하지 않으면 CLUB_NOT_EXISTS 예외 발생")
    void submitClubApplication_clubNotFound_throwsClubNotExists() {
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthentication();

            given(clubRepository.findByClubUUID(clubUUID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clubApplicationService.submitClubApplication(clubUUID))
                    .isInstanceOf(ClubException.class)
                    .extracting(e -> ((ClubException) e).getExceptionType())
                    .isEqualTo(ExceptionType.CLUB_NOT_EXISTS);
        }
    }

    @Test
    @DisplayName("DB 유니크 제약 위반 시 ALREADY_APPLIED 예외로 변환")
    void submitClubApplication_dataIntegrityViolation_throwsAlreadyApplied() {
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthentication();

            given(clubRepository.findByClubUUID(clubUUID)).willReturn(Optional.of(club));
            given(clubApplicationRepository.saveAndFlush(any(ClubApplication.class)))
                    .willThrow(new DataIntegrityViolationException("unique constraint violation"));

            assertThatThrownBy(() -> clubApplicationService.submitClubApplication(clubUUID))
                    .isInstanceOf(ClubApplicationException.class)
                    .extracting(e -> ((ClubApplicationException) e).getExceptionType())
                    .isEqualTo(ExceptionType.ALREADY_APPLIED);
        }
    }

    @Test
    @DisplayName("지원서 정상 제출 시 save 호출 및 예외 없음")
    void submitClubApplication_success_savesApplication() {
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            mockAuthentication();

            given(clubRepository.findByClubUUID(clubUUID)).willReturn(Optional.of(club));
            given(clubApplicationRepository.saveAndFlush(any(ClubApplication.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            clubApplicationService.submitClubApplication(clubUUID);

            verify(clubApplicationRepository, times(1)).saveAndFlush(any(ClubApplication.class));
        }
    }
}
