package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.admin.admin.dto.AdminClubListResponse;
import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.ClubCategory;
import com.USWCicrcleLink.server.club.domain.ClubCategoryMapping;
import com.USWCicrcleLink.server.club.domain.ClubHashtag;
import com.USWCicrcleLink.server.club.domain.ClubMainPhoto;
import com.USWCicrcleLink.server.club.domain.ClubMembers;
import com.USWCicrcleLink.server.club.domain.Department;
import com.USWCicrcleLink.server.club.domain.RecruitmentStatus;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import com.USWCicrcleLink.server.clubApplication.repository.ClubApplicationRepository;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntro;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntroPhoto;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroPhotoRepository;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroRepository;
import com.USWCicrcleLink.server.clubLeader.domain.Leader;
import com.USWCicrcleLink.server.clubLeader.repository.LeaderRepository;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.profile.domain.MemberType;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import com.USWCicrcleLink.server.user.domain.ExistingMember.ClubMemberAccountStatus;
import com.USWCicrcleLink.server.user.domain.ExistingMember.ClubMemberTemp;
import com.USWCicrcleLink.server.user.repository.ClubMemberAccountStatusRepository;
import com.USWCicrcleLink.server.user.repository.ClubMemberTempRepository;
import com.USWCicrcleLink.server.user.domain.User;
import com.USWCicrcleLink.server.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClubRepositoryCustomImplTest {

    private static final String USER_PASSWORD = "encoded-password";
    private static final String MAJOR = "컴퓨터공학";
    private static final String LEADER_NAME = "회장";
    private static final String LEADER_HP = "01099998888";
    private static final String CLUB_INSTA = "@club";
    private static final String INTRO_TEXT = "동아리 소개";
    private static final String RECRUITMENT_TEXT = "모집 안내";
    private static final String GOOGLE_FORM_URL = "https://forms.google.com/test";
    private static final String MAIN_S3_KEY = "club/main.jpg";
    private static final String INTRO_S3_KEY = "club/intro-1.jpg";

    @Autowired private ClubRepository clubRepository;
    @Autowired private ClubMembersRepository clubMembersRepository;
    @Autowired private LeaderRepository leaderRepository;
    @Autowired private ClubMainPhotoRepository clubMainPhotoRepository;
    @Autowired private ClubIntroRepository clubIntroRepository;
    @Autowired private ClubIntroPhotoRepository clubIntroPhotoRepository;
    @Autowired private ClubApplicationRepository clubApplicationRepository;
    @Autowired private ClubHashtagRepository clubHashtagRepository;
    @Autowired private ClubCategoryRepository clubCategoryRepository;
    @Autowired private ClubCategoryMappingRepository clubCategoryMappingRepository;
    @Autowired private ClubMemberTempRepository clubMemberTempRepository;
    @Autowired private ClubMemberAccountStatusRepository clubMemberAccountStatusRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private EntityManager entityManager;

    private Club targetClub;
    private Club secondClub;
    private Profile firstProfile;
    private Profile secondProfile;
    private Profile applicationProfile;

    @BeforeEach
    void setUp() {
        firstProfile = createProfile("user01", "user01@test.com", "홍길동", "20200001", "01012345678");
        secondProfile = createProfile("user02", "user02@test.com", "김철수", "20200002", "01023456789");
        applicationProfile = createProfile("user03", "user03@test.com", "이영희", "20200003", "01034567890");

        targetClub = clubRepository.save(Club.builder()
                .clubUUID(UUID.randomUUID())
                .clubName("밴드")
                .leaderName(LEADER_NAME)
                .leaderHp(LEADER_HP)
                .clubInsta(CLUB_INSTA)
                .department(Department.ART)
                .clubRoomNumber("A101")
                .build());
        secondClub = clubRepository.save(Club.builder()
                .clubUUID(UUID.randomUUID())
                .clubName("축구")
                .leaderName(LEADER_NAME)
                .leaderHp(LEADER_HP)
                .clubInsta(CLUB_INSTA)
                .department(Department.SPORT)
                .clubRoomNumber("B202")
                .build());
    }

    private Profile createProfile(String userAccount, String email, String userName, String studentNumber, String userHp) {
        User savedUser = userRepository.save(User.builder()
                .userUUID(UUID.randomUUID())
                .userAccount(userAccount)
                .userPw(USER_PASSWORD)
                .email(email)
                .userCreatedAt(LocalDateTime.of(2026, 3, 17, 12, 0))
                .userUpdatedAt(LocalDateTime.of(2026, 3, 17, 12, 0))
                .role(Role.USER)
                .build());

        return profileRepository.save(Profile.builder()
                .user(savedUser)
                .userName(userName)
                .studentNumber(studentNumber)
                .userHp(userHp)
                .major(MAJOR)
                .profileCreatedAt(LocalDateTime.of(2026, 3, 17, 12, 0))
                .profileUpdatedAt(LocalDateTime.of(2026, 3, 17, 12, 0))
                .memberType(MemberType.REGULARMEMBER)
                .build());
    }

    private void clearPersistenceContext() {
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    class findAllWithMemberAndLeaderCount_테스트 {

        @Test
        void 페이징과_회원수_및_리더_포함_집계를_정상_반환한다() {
            leaderRepository.save(Leader.builder()
                    .leaderAccount("leader01")
                    .leaderPw(USER_PASSWORD)
                    .club(targetClub)
                    .role(Role.LEADER)
                    .build());
            clubMembersRepository.save(ClubMembers.builder()
                    .clubMemberUUID(UUID.randomUUID())
                    .club(targetClub)
                    .profile(firstProfile)
                    .build());
            clubMembersRepository.save(ClubMembers.builder()
                    .clubMemberUUID(UUID.randomUUID())
                    .club(targetClub)
                    .profile(secondProfile)
                    .build());
            clubMembersRepository.save(ClubMembers.builder()
                    .clubMemberUUID(UUID.randomUUID())
                    .club(secondClub)
                    .profile(applicationProfile)
                    .build());

            Page<AdminClubListResponse> result = clubRepository.findAllWithMemberAndLeaderCount(PageRequest.of(0, 10));

            assertThat(result.getContent()).hasSize(2);
            AdminClubListResponse firstClub = result.getContent().stream()
                    .filter(item -> item.getClubUUID().equals(targetClub.getClubUUID()))
                    .findFirst()
                    .orElseThrow();
            AdminClubListResponse otherClub = result.getContent().stream()
                    .filter(item -> item.getClubUUID().equals(secondClub.getClubUUID()))
                    .findFirst()
                    .orElseThrow();
            assertThat(firstClub.getNumberOfClubMembers()).isEqualTo(3L);
            assertThat(otherClub.getNumberOfClubMembers()).isEqualTo(1L);
            assertThat(result.getTotalElements()).isEqualTo(2L);
            assertThat(result.getTotalPages()).isEqualTo(1);
        }

        @Test
        void 데이터가_없으면_빈_페이지를_반환한다() {
            clubRepository.deleteAll();

            Page<AdminClubListResponse> result = clubRepository.findAllWithMemberAndLeaderCount(PageRequest.of(0, 10));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    @Nested
    class deleteClubAndDependencies_테스트 {

        @Test
        void 연관_엔티티를_모두_삭제한다() {
            ClubIntro savedClubIntro = clubIntroRepository.save(ClubIntro.builder()
                    .club(targetClub)
                    .clubIntro(INTRO_TEXT)
                    .clubRecruitment(RECRUITMENT_TEXT)
                    .googleFormUrl(GOOGLE_FORM_URL)
                    .recruitmentStatus(RecruitmentStatus.OPEN)
                    .build());
            leaderRepository.save(Leader.builder()
                    .leaderAccount("leader-delete")
                    .leaderPw(USER_PASSWORD)
                    .club(targetClub)
                    .role(Role.LEADER)
                    .build());
            clubMainPhotoRepository.save(ClubMainPhoto.builder()
                    .club(targetClub)
                    .clubMainPhotoName("main.jpg")
                    .clubMainPhotoS3Key(MAIN_S3_KEY)
                    .build());
            clubIntroPhotoRepository.save(ClubIntroPhoto.builder()
                    .clubIntro(savedClubIntro)
                    .clubIntroPhotoName("intro.jpg")
                    .clubIntroPhotoS3Key(INTRO_S3_KEY)
                    .order(1)
                    .build());
            clubHashtagRepository.save(ClubHashtag.builder()
                    .club(targetClub)
                    .clubHashtag("공연")
                    .build());
            ClubCategory category = clubCategoryRepository.save(ClubCategory.builder()
                    .clubCategoryUUID(UUID.randomUUID())
                    .clubCategoryName("예술")
                    .build());
            clubCategoryMappingRepository.save(ClubCategoryMapping.builder()
                    .club(targetClub)
                    .clubCategory(category)
                    .build());
            clubMembersRepository.save(ClubMembers.builder()
                    .clubMemberUUID(UUID.randomUUID())
                    .club(targetClub)
                    .profile(firstProfile)
                    .build());
            clubApplicationRepository.save(ClubApplication.builder()
                    .club(targetClub)
                    .profile(applicationProfile)
                    .clubApplicationUUID(UUID.randomUUID())
                    .submittedAt(LocalDateTime.of(2026, 3, 17, 13, 0))
                    .clubApplicationStatus(ClubApplicationStatus.WAIT)
                    .checked(false)
                    .build());
            ClubMemberTemp clubMemberTemp = clubMemberTempRepository.save(ClubMemberTemp.builder()
                    .profileTempAccount("temp01")
                    .profileTempPw(USER_PASSWORD)
                    .profileTempName("임시회원")
                    .profileTempStudentNumber("20209999")
                    .profileTempHp("01055556666")
                    .profileTempMajor(MAJOR)
                    .profileTempEmail("temp@test.com")
                    .totalClubRequest(1)
                    .clubMemberTempExpiryDate(LocalDateTime.of(2026, 3, 18, 12, 0))
                    .build());
            clubMemberAccountStatusRepository.save(ClubMemberAccountStatus.builder()
                    .clubMemberAccountStatusUUID(UUID.randomUUID())
                    .club(targetClub)
                    .clubMemberTemp(clubMemberTemp)
                    .build());

            clubRepository.deleteClubAndDependencies(targetClub.getClubId());
            clearPersistenceContext();

            assertThat(clubRepository.findById(targetClub.getClubId())).isEmpty();
            assertThat(leaderRepository.findByClubUUID(targetClub.getClubUUID())).isEmpty();
            assertThat(clubMainPhotoRepository.findByClubClubId(targetClub.getClubId())).isEmpty();
            assertThat(clubIntroRepository.findByClubClubId(targetClub.getClubId())).isEmpty();
            assertThat(clubIntroPhotoRepository.findByClubIntroClubId(targetClub.getClubId())).isEmpty();
            assertThat(clubHashtagRepository.findByClubClubId(targetClub.getClubId())).isEmpty();
            assertThat(clubCategoryMappingRepository.findByClubClubId(targetClub.getClubId())).isEmpty();
            assertThat(clubMembersRepository.findByClub(targetClub)).isEmpty();
            assertThat(clubApplicationRepository.findByClub_ClubIdAndChecked(targetClub.getClubId(), false)).isEmpty();
            assertThat(clubMemberAccountStatusRepository.findAll()).isEmpty();
        }

        @Test
        void 사진_키가_없어도_연관_엔티티를_정상_삭제한다() {
            leaderRepository.save(Leader.builder()
                    .leaderAccount("leader-no-photo")
                    .leaderPw(USER_PASSWORD)
                    .club(targetClub)
                    .role(Role.LEADER)
                    .build());
            clubMembersRepository.save(ClubMembers.builder()
                    .clubMemberUUID(UUID.randomUUID())
                    .club(targetClub)
                    .profile(firstProfile)
                    .build());

            clubRepository.deleteClubAndDependencies(targetClub.getClubId());
            clearPersistenceContext();

            assertThat(clubRepository.findById(targetClub.getClubId())).isEmpty();
            assertThat(leaderRepository.findByClubUUID(targetClub.getClubUUID())).isEmpty();
            assertThat(clubMembersRepository.findByClub(targetClub)).isEmpty();
        }
    }
}
