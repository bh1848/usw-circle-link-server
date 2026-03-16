package com.USWCicrcleLink.server.clubApplication.repository;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.Department;
import com.USWCicrcleLink.server.club.repository.ClubRepository;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.profile.domain.MemberType;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import com.USWCicrcleLink.server.user.domain.User;
import com.USWCicrcleLink.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClubApplicationRepositoryTest {

    private static final String USER_ACCOUNT = "user01";
    private static final String USER_PASSWORD = "encoded-password";
    private static final String USER_EMAIL = "user01@test.com";
    private static final String USER_NAME = "홍길동";
    private static final String STUDENT_NUMBER = "20200001";
    private static final String USER_HP = "01012345678";
    private static final String MAJOR = "컴퓨터공학";
    private static final String CLUB_NAME = "밴드";
    private static final String LEADER_NAME = "회장";
    private static final String LEADER_HP = "01099998888";
    private static final String CLUB_INSTA = "@band";
    private static final String CLUB_ROOM = "A101";

    @Autowired
    private ClubApplicationRepository clubApplicationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ClubRepository clubRepository;

    @MockBean
    private com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService s3FileUploadService;

    private Profile savedProfile;
    private Club savedClub;
    private ClubApplication savedClubApplication;

    @BeforeEach
    void setUp() {
        savedProfile = createProfile(
                USER_ACCOUNT,
                USER_EMAIL,
                USER_NAME,
                STUDENT_NUMBER,
                USER_HP
        );
        savedClub = clubRepository.save(Club.builder()
                .clubUUID(UUID.randomUUID())
                .clubName(CLUB_NAME)
                .leaderName(LEADER_NAME)
                .leaderHp(LEADER_HP)
                .clubInsta(CLUB_INSTA)
                .department(Department.ART)
                .clubRoomNumber(CLUB_ROOM)
                .build());
        savedClubApplication = clubApplicationRepository.save(ClubApplication.builder()
                .club(savedClub)
                .profile(savedProfile)
                .clubApplicationUUID(UUID.randomUUID())
                .submittedAt(LocalDateTime.of(2026, 3, 17, 12, 0))
                .clubApplicationStatus(ClubApplicationStatus.WAIT)
                .checked(false)
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

    @Nested
    class existsByProfileAndClubUUID_테스트 {

        @Test
        void 지원한_프로필과_동아리_UUID가_있으면_true를_반환한다() {
            boolean result = clubApplicationRepository.existsByProfileAndClubUUID(savedProfile, savedClub.getClubUUID());

            assertThat(result).isTrue();
        }

        @Test
        void 지원한_데이터가_없으면_false를_반환한다() {
            Profile anotherProfile = createProfile(
                    "user02",
                    "user02@test.com",
                    "김철수",
                    "20200002",
                    "01011112222"
            );

            boolean result = clubApplicationRepository.existsByProfileAndClubUUID(anotherProfile, savedClub.getClubUUID());

            assertThat(result).isFalse();
        }
    }

    @Nested
    class findClubByClubApplicationId_테스트 {

        @Test
        void 지원서_ID로_조회하면_동아리를_반환한다() {
            Optional<Club> result = clubApplicationRepository.findClubByClubApplicationId(savedClubApplication.getClubApplicationId());

            assertThat(result).isPresent();
            assertThat(result.get().getClubId()).isEqualTo(savedClub.getClubId());
            assertThat(result.get().getClubUUID()).isEqualTo(savedClub.getClubUUID());
            assertThat(result.get().getClubName()).isEqualTo(CLUB_NAME);
        }

        @Test
        void 존재하지_않는_지원서_ID로_조회하면_빈_Optional을_반환한다() {
            Optional<Club> result = clubApplicationRepository.findClubByClubApplicationId(Long.MAX_VALUE);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class findAllWithProfileByClubId_테스트 {

        @Test
        void checked_조건에_맞는_지원서를_프로필과_함께_반환한다() {
            Profile checkedProfile = createProfile(
                    "user03",
                    "user03@test.com",
                    "이영희",
                    "20200003",
                    "01033334444"
            );
            clubApplicationRepository.save(ClubApplication.builder()
                    .club(savedClub)
                    .profile(checkedProfile)
                    .clubApplicationUUID(UUID.randomUUID())
                    .submittedAt(LocalDateTime.of(2026, 3, 17, 13, 0))
                    .clubApplicationStatus(ClubApplicationStatus.PASS)
                    .checked(true)
                    .build());

            List<ClubApplication> result = clubApplicationRepository.findAllWithProfileByClubId(savedClub.getClubId(), true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProfile().getUserName()).isEqualTo("이영희");
            assertThat(result.get(0).isChecked()).isTrue();
        }

        @Test
        void 조건에_맞는_지원서가_없으면_빈_리스트를_반환한다() {
            List<ClubApplication> result = clubApplicationRepository.findAllWithProfileByClubId(savedClub.getClubId(), true);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class findAllWithProfileByClubIdAndFailed_테스트 {

        @Test
        void checked와_status_조건에_맞는_지원서를_프로필과_함께_반환한다() {
            Profile failedProfile = createProfile(
                    "user04",
                    "user04@test.com",
                    "박민수",
                    "20200004",
                    "01055556666"
            );
            Profile passedProfile = createProfile(
                    "user05",
                    "user05@test.com",
                    "최지우",
                    "20200005",
                    "01077778888"
            );
            clubApplicationRepository.save(ClubApplication.builder()
                    .club(savedClub)
                    .profile(failedProfile)
                    .clubApplicationUUID(UUID.randomUUID())
                    .submittedAt(LocalDateTime.of(2026, 3, 17, 14, 0))
                    .clubApplicationStatus(ClubApplicationStatus.FAIL)
                    .checked(true)
                    .build());
            clubApplicationRepository.save(ClubApplication.builder()
                    .club(savedClub)
                    .profile(passedProfile)
                    .clubApplicationUUID(UUID.randomUUID())
                    .submittedAt(LocalDateTime.of(2026, 3, 17, 15, 0))
                    .clubApplicationStatus(ClubApplicationStatus.PASS)
                    .checked(true)
                    .build());

            List<ClubApplication> result = clubApplicationRepository.findAllWithProfileByClubIdAndFailed(
                    savedClub.getClubId(),
                    true,
                    ClubApplicationStatus.FAIL
            );

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProfile().getUserName()).isEqualTo("박민수");
            assertThat(result.get(0).getClubApplicationStatus()).isEqualTo(ClubApplicationStatus.FAIL);
            assertThat(result.get(0).isChecked()).isTrue();
        }

        @Test
        void 조건에_맞는_실패_지원서가_없으면_빈_리스트를_반환한다() {
            List<ClubApplication> result = clubApplicationRepository.findAllWithProfileByClubIdAndFailed(
                    savedClub.getClubId(),
                    true,
                    ClubApplicationStatus.FAIL
            );

            assertThat(result).isEmpty();
        }
    }
}
