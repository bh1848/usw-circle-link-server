package com.USWCicrcleLink.server.clubApplication.service;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.repository.ClubMembersRepository;
import com.USWCicrcleLink.server.club.repository.ClubRepository;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
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
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    private void mockAuthentication() {
        given(user.getUserUUID()).willReturn(userUUID);
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(new CustomUserDetails(user, List.of()));
        given(profileRepository.findByUser_UserUUID(userUUID)).willReturn(Optional.of(profile));
    }


    @Nested
    class checkIfCanApply_테스트 {

        @Test
        void 이미_지원한_경우_ALREADY_APPLIED_예외가_발생한다() {
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
        void 이미_동아리_회원인_경우_ALREADY_MEMBER_예외가_발생한다() {
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
        void 전화번호_중복인_경우_PHONE_NUMBER_ALREADY_REGISTERED_예외가_발생한다() {
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
        void 학번_중복인_경우_STUDENT_NUMBER_ALREADY_REGISTERED_예외가_발생한다() {
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
        void 모든_조건을_통과하면_예외_없이_정상_처리된다() {
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

                clubApplicationService.checkIfCanApply(clubUUID);
            }
        }
    }


    @Nested
    class getGoogleFormUrlByClubUUID_테스트 {

        @Test
        void 동아리_소개글이_없으면_CLUB_INTRO_NOT_EXISTS_예외가_발생한다() {
            given(clubIntroRepository.findByClubUUID(clubUUID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clubApplicationService.getGoogleFormUrlByClubUUID(clubUUID))
                    .isInstanceOf(ClubException.class)
                    .extracting(e -> ((ClubException) e).getExceptionType())
                    .isEqualTo(ExceptionType.CLUB_INTRO_NOT_EXISTS);
        }

        @Test
        void 구글_폼_URL이_null이면_GOOGLE_FORM_URL_NOT_EXISTS_예외가_발생한다() {
            ClubIntro clubIntro = mock(ClubIntro.class);
            given(clubIntroRepository.findByClubUUID(clubUUID)).willReturn(Optional.of(clubIntro));
            given(clubIntro.getGoogleFormUrl()).willReturn(null);

            assertThatThrownBy(() -> clubApplicationService.getGoogleFormUrlByClubUUID(clubUUID))
                    .isInstanceOf(ClubException.class)
                    .extracting(e -> ((ClubException) e).getExceptionType())
                    .isEqualTo(ExceptionType.GOOGLE_FORM_URL_NOT_EXISTS);
        }

        @Test
        void 구글_폼_URL이_빈_문자열이면_GOOGLE_FORM_URL_NOT_EXISTS_예외가_발생한다() {
            ClubIntro clubIntro = mock(ClubIntro.class);
            given(clubIntroRepository.findByClubUUID(clubUUID)).willReturn(Optional.of(clubIntro));
            given(clubIntro.getGoogleFormUrl()).willReturn("");

            assertThatThrownBy(() -> clubApplicationService.getGoogleFormUrlByClubUUID(clubUUID))
                    .isInstanceOf(ClubException.class)
                    .extracting(e -> ((ClubException) e).getExceptionType())
                    .isEqualTo(ExceptionType.GOOGLE_FORM_URL_NOT_EXISTS);
        }

        @Test
        void 구글_폼_URL을_정상_반환한다() {
            String expectedUrl = "https://forms.google.com/test";
            ClubIntro clubIntro = mock(ClubIntro.class);
            given(clubIntroRepository.findByClubUUID(clubUUID)).willReturn(Optional.of(clubIntro));
            given(clubIntro.getGoogleFormUrl()).willReturn(expectedUrl);

            String result = clubApplicationService.getGoogleFormUrlByClubUUID(clubUUID);

            assertThat(result).isEqualTo(expectedUrl);
        }
    }


    @Nested
    class submitClubApplication_테스트 {

        @Test
        void 동아리가_존재하지_않으면_CLUB_NOT_EXISTS_예외가_발생한다() {
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
        void DB_유니크_제약을_위반하면_ALREADY_APPLIED_예외로_변환된다() {
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
        void 지원서를_정상_제출하면_saveAndFlush를_호출한다() {
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
}
