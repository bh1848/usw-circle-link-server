package com.USWCicrcleLink.server.admin.admin.service;

import com.USWCicrcleLink.server.admin.admin.domain.Admin;
import com.USWCicrcleLink.server.admin.admin.dto.AdminClubCreationRequest;
import com.USWCicrcleLink.server.admin.admin.dto.AdminClubListResponse;
import com.USWCicrcleLink.server.admin.admin.dto.AdminClubPageListResponse;
import com.USWCicrcleLink.server.admin.admin.dto.AdminPwRequest;
import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.ClubMainPhoto;
import com.USWCicrcleLink.server.club.domain.Department;
import com.USWCicrcleLink.server.club.domain.RecruitmentStatus;
import com.USWCicrcleLink.server.club.repository.ClubMainPhotoRepository;
import com.USWCicrcleLink.server.club.repository.ClubRepository;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntro;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntroPhoto;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroPhotoRepository;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroRepository;
import com.USWCicrcleLink.server.clubLeader.domain.Leader;
import com.USWCicrcleLink.server.clubLeader.repository.LeaderRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.AdminException;
import com.USWCicrcleLink.server.global.exception.errortype.BaseException;
import com.USWCicrcleLink.server.global.exception.errortype.ClubException;
import com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService;
import com.USWCicrcleLink.server.global.security.details.CustomAdminDetails;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminClubServiceTest {

    private static final String LEADER_ACCOUNT = "leader01";
    private static final String LEADER_PASSWORD = "Leader!123";
    private static final String ADMIN_PASSWORD = "Admin!123";
    private static final String ENCODED_LEADER_PASSWORD = "encodedLeaderPw";
    private static final String ENCODED_ADMIN_PASSWORD = "encodedAdminPw";
    private static final String CLUB_NAME = "동아리";
    private static final String CLUB_ROOM_NUMBER = "A101";

    @Mock private LeaderRepository leaderRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private ClubIntroRepository clubIntroRepository;
    @Mock private ClubMainPhotoRepository clubMainPhotoRepository;
    @Mock private ClubIntroPhotoRepository clubIntroPhotoRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private S3FileUploadService s3FileUploadService;

    @InjectMocks
    private AdminClubService adminClubService;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    @Captor
    private ArgumentCaptor<List<ClubIntroPhoto>> clubIntroPhotosCaptor;
    @Captor
    private ArgumentCaptor<TransactionSynchronization> transactionSynchronizationCaptor;

    private Admin admin;
    private UUID clubUUID;

    @BeforeEach
    void setUp() {
        admin = Admin.builder()
                .adminUUID(UUID.randomUUID())
                .adminAccount("admin")
                .adminPw(ENCODED_ADMIN_PASSWORD)
                .adminName("관리자")
                .role(Role.ADMIN)
                .build();
        clubUUID = UUID.randomUUID();
    }

    private void mockAuthentication() {
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(new CustomAdminDetails(admin));
    }

    private AdminClubCreationRequest createRequest() {
        return new AdminClubCreationRequest(
                LEADER_ACCOUNT,
                LEADER_PASSWORD,
                LEADER_PASSWORD,
                CLUB_NAME,
                Department.ART,
                ADMIN_PASSWORD,
                CLUB_ROOM_NUMBER
        );
    }

    @Nested
    class getAllClubs_테스트 {

        @Test
        void 동아리_목록_페이지를_정상_반환한다() {
            Pageable pageable = PageRequest.of(1, 2);
            List<AdminClubListResponse> clubs = List.of(
                    new AdminClubListResponse(UUID.randomUUID(), Department.ART, "밴드", "홍길동", 10L),
                    new AdminClubListResponse(UUID.randomUUID(), Department.SPORT, "축구", "김철수", 20L)
            );
            given(clubRepository.findAllWithMemberAndLeaderCount(pageable))
                    .willReturn(new PageImpl<>(clubs, pageable, 4));

            AdminClubPageListResponse result = adminClubService.getAllClubs(pageable);

            assertThat(result.getContent()).isEqualTo(clubs);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.getTotalElements()).isEqualTo(4);
            assertThat(result.getCurrentPage()).isEqualTo(1);
        }
    }

    @Nested
    class createClub_테스트 {

        @Test
        void 회장_비밀번호_확인이_다르면_ClUB_LEADER_PASSWORD_NOT_MATCH_예외가_발생한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminClubCreationRequest request = new AdminClubCreationRequest(
                        LEADER_ACCOUNT,
                        LEADER_PASSWORD,
                        "different!123",
                        CLUB_NAME,
                        Department.ART,
                        ADMIN_PASSWORD,
                        CLUB_ROOM_NUMBER
                );

                assertThatThrownBy(() -> adminClubService.createClub(request))
                        .isInstanceOf(ClubException.class)
                        .extracting(e -> ((ClubException) e).getExceptionType())
                        .isEqualTo(ExceptionType.ClUB_LEADER_PASSWORD_NOT_MATCH);
            }
        }

        @Test
        void 회장_계정이_중복되면_LEADER_ACCOUNT_ALREADY_EXISTS_예외가_발생한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminClubCreationRequest request = createRequest();
                given(leaderRepository.existsByLeaderAccount(LEADER_ACCOUNT)).willReturn(true);

                assertThatThrownBy(() -> adminClubService.createClub(request))
                        .isInstanceOf(ClubException.class)
                        .extracting(e -> ((ClubException) e).getExceptionType())
                        .isEqualTo(ExceptionType.LEADER_ACCOUNT_ALREADY_EXISTS);
            }
        }

        @Test
        void 동아리명이_중복되면_CLUB_NAME_ALREADY_EXISTS_예외가_발생한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminClubCreationRequest request = createRequest();
                given(leaderRepository.existsByLeaderAccount(LEADER_ACCOUNT)).willReturn(false);
                given(clubRepository.existsByClubName(CLUB_NAME)).willReturn(true);

                assertThatThrownBy(() -> adminClubService.createClub(request))
                        .isInstanceOf(ClubException.class)
                        .extracting(e -> ((ClubException) e).getExceptionType())
                        .isEqualTo(ExceptionType.CLUB_NAME_ALREADY_EXISTS);
            }
        }

        @Test
        void 관리자_비밀번호가_다르면_ADMIN_PASSWORD_NOT_MATCH_예외가_발생한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminClubCreationRequest request = createRequest();
                given(leaderRepository.existsByLeaderAccount(LEADER_ACCOUNT)).willReturn(false);
                given(clubRepository.existsByClubName(CLUB_NAME)).willReturn(false);
                given(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_ADMIN_PASSWORD)).willReturn(false);

                assertThatThrownBy(() -> adminClubService.createClub(request))
                        .isInstanceOf(AdminException.class)
                        .extracting(e -> ((AdminException) e).getExceptionType())
                        .isEqualTo(ExceptionType.ADMIN_PASSWORD_NOT_MATCH);
            }
        }

        @Test
        void 모든_검증을_통과하면_동아리와_기본_데이터를_생성한다() {
            ArgumentCaptor<Club> clubCaptor = ArgumentCaptor.forClass(Club.class);
            ArgumentCaptor<Leader> leaderCaptor = ArgumentCaptor.forClass(Leader.class);
            ArgumentCaptor<ClubMainPhoto> clubMainPhotoCaptor = ArgumentCaptor.forClass(ClubMainPhoto.class);
            ArgumentCaptor<ClubIntro> clubIntroCaptor = ArgumentCaptor.forClass(ClubIntro.class);

            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminClubCreationRequest request = createRequest();
                given(leaderRepository.existsByLeaderAccount(LEADER_ACCOUNT)).willReturn(false);
                given(clubRepository.existsByClubName(CLUB_NAME)).willReturn(false);
                given(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_ADMIN_PASSWORD)).willReturn(true);
                given(passwordEncoder.encode(LEADER_PASSWORD)).willReturn(ENCODED_LEADER_PASSWORD);
                given(clubRepository.save(any(Club.class))).willAnswer(invocation -> invocation.getArgument(0));
                given(clubIntroRepository.save(any(ClubIntro.class))).willAnswer(invocation -> invocation.getArgument(0));

                adminClubService.createClub(request);

                verify(clubRepository).save(clubCaptor.capture());
                verify(leaderRepository).save(leaderCaptor.capture());
                verify(clubMainPhotoRepository).save(clubMainPhotoCaptor.capture());
                verify(clubIntroRepository).save(clubIntroCaptor.capture());
                verify(clubIntroPhotoRepository).saveAll(clubIntroPhotosCaptor.capture());

                Club savedClub = clubCaptor.getValue();
                assertThat(savedClub.getClubName()).isEqualTo(CLUB_NAME);
                assertThat(savedClub.getDepartment()).isEqualTo(Department.ART);
                assertThat(savedClub.getLeaderName()).isEmpty();
                assertThat(savedClub.getLeaderHp()).isEmpty();
                assertThat(savedClub.getClubInsta()).isEmpty();
                assertThat(savedClub.getClubRoomNumber()).isEqualTo(CLUB_ROOM_NUMBER);

                Leader savedLeader = leaderCaptor.getValue();
                assertThat(savedLeader.getLeaderAccount()).isEqualTo(LEADER_ACCOUNT);
                assertThat(savedLeader.getLeaderPw()).isEqualTo(ENCODED_LEADER_PASSWORD);
                assertThat(savedLeader.getRole()).isEqualTo(Role.LEADER);
                assertThat(savedLeader.getClub()).isSameAs(savedClub);
                assertThat(savedLeader.getLeaderUUID()).isNotNull();

                ClubMainPhoto savedClubMainPhoto = clubMainPhotoCaptor.getValue();
                assertThat(savedClubMainPhoto.getClub()).isSameAs(savedClub);
                assertThat(savedClubMainPhoto.getClubMainPhotoName()).isEmpty();
                assertThat(savedClubMainPhoto.getClubMainPhotoS3Key()).isEmpty();

                ClubIntro savedClubIntro = clubIntroCaptor.getValue();
                assertThat(savedClubIntro.getClub()).isSameAs(savedClub);
                assertThat(savedClubIntro.getClubIntro()).isEmpty();
                assertThat(savedClubIntro.getGoogleFormUrl()).isEmpty();
                assertThat(savedClubIntro.getRecruitmentStatus()).isEqualTo(RecruitmentStatus.CLOSE);

                List<ClubIntroPhoto> savedIntroPhotos = clubIntroPhotosCaptor.getValue();
                assertThat(savedIntroPhotos).hasSize(5);
                assertThat(savedIntroPhotos)
                        .extracting(ClubIntroPhoto::getClubIntro)
                        .containsOnly(savedClubIntro);
                assertThat(savedIntroPhotos)
                        .extracting(ClubIntroPhoto::getOrder)
                        .containsExactly(1, 2, 3, 4, 5);
                assertThat(savedIntroPhotos)
                        .extracting(ClubIntroPhoto::getClubIntroPhotoName)
                        .containsOnly("");
                assertThat(savedIntroPhotos)
                        .extracting(ClubIntroPhoto::getClubIntroPhotoS3Key)
                        .containsOnly("");
            }
        }
    }

    @Nested
    class validateLeaderAccount_테스트 {

        @Test
        void 회장_계정이_이미_존재하면_LEADER_ACCOUNT_ALREADY_EXISTS_예외가_발생한다() {
            given(leaderRepository.existsByLeaderAccount(LEADER_ACCOUNT)).willReturn(true);

            assertThatThrownBy(() -> adminClubService.validateLeaderAccount(LEADER_ACCOUNT))
                    .isInstanceOf(ClubException.class)
                    .extracting(e -> ((ClubException) e).getExceptionType())
                    .isEqualTo(ExceptionType.LEADER_ACCOUNT_ALREADY_EXISTS);
        }

        @Test
        void 회장_계정이_중복되지_않으면_정상_통과한다() {
            given(leaderRepository.existsByLeaderAccount(LEADER_ACCOUNT)).willReturn(false);

            adminClubService.validateLeaderAccount(LEADER_ACCOUNT);
        }
    }

    @Nested
    class validateClubName_테스트 {

        @Test
        void 동아리명이_이미_존재하면_CLUB_NAME_ALREADY_EXISTS_예외가_발생한다() {
            given(clubRepository.existsByClubName(CLUB_NAME)).willReturn(true);

            assertThatThrownBy(() -> adminClubService.validateClubName(CLUB_NAME))
                    .isInstanceOf(ClubException.class)
                    .extracting(e -> ((ClubException) e).getExceptionType())
                    .isEqualTo(ExceptionType.CLUB_NAME_ALREADY_EXISTS);
        }

        @Test
        void 동아리명이_중복되지_않으면_정상_통과한다() {
            given(clubRepository.existsByClubName(CLUB_NAME)).willReturn(false);

            adminClubService.validateClubName(CLUB_NAME);
        }
    }

    @Nested
    class deleteClub_테스트 {

        @Test
        void 관리자_비밀번호가_다르면_ADMIN_PASSWORD_NOT_MATCH_예외가_발생한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminPwRequest request = new AdminPwRequest(ADMIN_PASSWORD);
                given(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_ADMIN_PASSWORD)).willReturn(false);

                assertThatThrownBy(() -> adminClubService.deleteClub(clubUUID, request))
                        .isInstanceOf(AdminException.class)
                        .extracting(e -> ((AdminException) e).getExceptionType())
                        .isEqualTo(ExceptionType.ADMIN_PASSWORD_NOT_MATCH);
            }
        }

        @Test
        void 동아리_UUID가_없으면_CLUB_NOT_EXISTS_예외가_발생한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminPwRequest request = new AdminPwRequest(ADMIN_PASSWORD);
                given(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_ADMIN_PASSWORD)).willReturn(true);
                given(clubRepository.findClubIdByClubUUID(clubUUID)).willReturn(Optional.empty());

                assertThatThrownBy(() -> adminClubService.deleteClub(clubUUID, request))
                        .isInstanceOf(ClubException.class)
                        .extracting(e -> ((ClubException) e).getExceptionType())
                        .isEqualTo(ExceptionType.CLUB_NOT_EXISTS);
            }
        }

        @Test
        void 삭제_대상_동아리가_있으면_커밋_후_S3_파일과_함께_삭제한다() {
            ClubIntroPhoto firstIntroPhoto = ClubIntroPhoto.builder().clubIntroPhotoS3Key("intro-1.jpg").build();
            ClubIntroPhoto emptyIntroPhoto = ClubIntroPhoto.builder().clubIntroPhotoS3Key("").build();
            ClubIntroPhoto secondIntroPhoto = ClubIntroPhoto.builder().clubIntroPhotoS3Key("intro-2.jpg").build();

            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class);
                 MockedStatic<TransactionSynchronizationManager> transactionMocked = mockStatic(TransactionSynchronizationManager.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminPwRequest request = new AdminPwRequest(ADMIN_PASSWORD);
                given(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_ADMIN_PASSWORD)).willReturn(true);
                given(clubRepository.findClubIdByClubUUID(clubUUID)).willReturn(Optional.of(1L));
                given(clubMainPhotoRepository.findS3KeyByClubId(1L)).willReturn(Optional.of("main.jpg"));
                given(clubIntroPhotoRepository.findByClubIntroClubId(1L))
                        .willReturn(List.of(firstIntroPhoto, emptyIntroPhoto, secondIntroPhoto));

                adminClubService.deleteClub(clubUUID, request);

                verify(clubRepository).deleteClubAndDependencies(1L);
                transactionMocked.verify(() -> TransactionSynchronizationManager.registerSynchronization(transactionSynchronizationCaptor.capture()));
                transactionSynchronizationCaptor.getValue().afterCommit();
                verify(s3FileUploadService).deleteFiles(List.of("main.jpg", "intro-1.jpg", "intro-2.jpg"));
            }
        }

        @Test
        void 삭제할_S3_키가_없으면_afterCommit을_등록하지_않는다() {
            ClubIntroPhoto emptyIntroPhoto = ClubIntroPhoto.builder().clubIntroPhotoS3Key("").build();
            ClubIntroPhoto blankIntroPhoto = ClubIntroPhoto.builder().clubIntroPhotoS3Key("   ").build();

            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class);
                 MockedStatic<TransactionSynchronizationManager> transactionMocked = mockStatic(TransactionSynchronizationManager.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminPwRequest request = new AdminPwRequest(ADMIN_PASSWORD);
                given(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_ADMIN_PASSWORD)).willReturn(true);
                given(clubRepository.findClubIdByClubUUID(clubUUID)).willReturn(Optional.of(1L));
                given(clubMainPhotoRepository.findS3KeyByClubId(1L)).willReturn(Optional.of(""));
                given(clubIntroPhotoRepository.findByClubIntroClubId(1L))
                        .willReturn(List.of(emptyIntroPhoto, blankIntroPhoto));

                adminClubService.deleteClub(clubUUID, request);

                verify(clubRepository).deleteClubAndDependencies(1L);
                transactionMocked.verifyNoInteractions();
                verify(s3FileUploadService, org.mockito.Mockito.never()).deleteFiles(any());
            }
        }

        @Test
        void 삭제_중_예외가_발생하면_SERVER_ERROR_예외로_변환한다() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminPwRequest request = new AdminPwRequest(ADMIN_PASSWORD);
                RuntimeException runtimeException = new RuntimeException("delete failed");
                given(passwordEncoder.matches(ADMIN_PASSWORD, ENCODED_ADMIN_PASSWORD)).willReturn(true);
                given(clubRepository.findClubIdByClubUUID(clubUUID)).willReturn(Optional.of(1L));
                doThrow(runtimeException).when(clubRepository).deleteClubAndDependencies(1L);

                assertThatThrownBy(() -> adminClubService.deleteClub(clubUUID, request))
                        .isInstanceOf(BaseException.class)
                        .extracting(e -> ((BaseException) e).getExceptionType())
                        .isEqualTo(ExceptionType.SERVER_ERROR);
            }
        }
    }
}
