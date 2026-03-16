package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.ClubMembers;
import com.USWCicrcleLink.server.club.domain.Department;
import com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ClubMembersRepositoryTest {

    private static final String USER_PASSWORD = "encoded-password";
    private static final String MAJOR = "컴퓨터공학";
    private static final String CLUB_NAME = "밴드";
    private static final String SECOND_CLUB_NAME = "축구";
    private static final String LEADER_NAME = "회장";
    private static final String LEADER_HP = "01099998888";
    private static final String CLUB_INSTA = "@club";
    private static final String CLUB_ROOM = "A101";

    @Autowired
    private ClubMembersRepository clubMembersRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private ClubRepository clubRepository;

    @MockBean
    private S3FileUploadService s3FileUploadService;

    private Profile savedProfile;
    private Profile secondProfile;
    private Profile profileWithoutClub;
    private Club savedClub;
    private Club secondClub;

    @BeforeEach
    void setUp() {
        savedProfile = createProfile("user01", "user01@test.com", "홍길동", "20200001", "01012345678");
        secondProfile = createProfile("user02", "user02@test.com", "김철수", "20200002", "01023456789");
        profileWithoutClub = createProfile("user03", "user03@test.com", "이영희", "20200003", "01034567890");

        savedClub = clubRepository.save(Club.builder()
                .clubUUID(UUID.randomUUID())
                .clubName(CLUB_NAME)
                .leaderName(LEADER_NAME)
                .leaderHp(LEADER_HP)
                .clubInsta(CLUB_INSTA)
                .department(Department.ART)
                .clubRoomNumber(CLUB_ROOM)
                .build());
        secondClub = clubRepository.save(Club.builder()
                .clubUUID(UUID.randomUUID())
                .clubName(SECOND_CLUB_NAME)
                .leaderName(LEADER_NAME)
                .leaderHp(LEADER_HP)
                .clubInsta(CLUB_INSTA)
                .department(Department.SPORT)
                .clubRoomNumber("B202")
                .build());

        clubMembersRepository.save(ClubMembers.builder()
                .clubMemberUUID(UUID.randomUUID())
                .profile(savedProfile)
                .club(savedClub)
                .build());
        clubMembersRepository.save(ClubMembers.builder()
                .clubMemberUUID(UUID.randomUUID())
                .profile(secondProfile)
                .club(secondClub)
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
        void 동아리_회원이면_true를_반환한다() {
            boolean result = clubMembersRepository.existsByProfileAndClubUUID(savedProfile, savedClub.getClubUUID());

            assertThat(result).isTrue();
        }

        @Test
        void 동아리_회원이_아니면_false를_반환한다() {
            boolean result = clubMembersRepository.existsByProfileAndClubUUID(profileWithoutClub, savedClub.getClubUUID());

            assertThat(result).isFalse();
        }
    }

    @Nested
    class findProfilesByClubUUID_테스트 {

        @Test
        void 동아리_UUID로_소속_프로필_목록을_반환한다() {
            List<Profile> result = clubMembersRepository.findProfilesByClubUUID(savedClub.getClubUUID());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getProfileId()).isEqualTo(savedProfile.getProfileId());
            assertThat(result.get(0).getUserName()).isEqualTo("홍길동");
        }

        @Test
        void 소속_프로필이_없으면_빈_리스트를_반환한다() {
            List<Profile> result = clubMembersRepository.findProfilesByClubUUID(UUID.randomUUID());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class findClubUUIDsByProfileId_테스트 {

        @Test
        void 프로필_ID로_소속_동아리_UUID_목록을_반환한다() {
            List<UUID> result = clubMembersRepository.findClubUUIDsByProfileId(savedProfile.getProfileId());

            assertThat(result).containsExactly(savedClub.getClubUUID());
        }

        @Test
        void 소속_동아리가_없으면_빈_리스트를_반환한다() {
            List<UUID> result = clubMembersRepository.findClubUUIDsByProfileId(profileWithoutClub.getProfileId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class findAllWithProfileByClubClubId_테스트 {

        @Test
        void 동아리_ID로_회원과_프로필을_함께_반환한다() {
            List<ClubMembers> result = clubMembersRepository.findAllWithProfileByClubClubId(savedClub.getClubId());

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getClub().getClubId()).isEqualTo(savedClub.getClubId());
            assertThat(result.get(0).getProfile().getUserName()).isEqualTo("홍길동");
        }

        @Test
        void 해당_동아리의_회원이_없으면_빈_리스트를_반환한다() {
            Club emptyClub = clubRepository.save(Club.builder()
                    .clubUUID(UUID.randomUUID())
                    .clubName("영화")
                    .leaderName(LEADER_NAME)
                    .leaderHp(LEADER_HP)
                    .clubInsta(CLUB_INSTA)
                    .department(Department.SHOW)
                    .clubRoomNumber("C303")
                    .build());

            List<ClubMembers> result = clubMembersRepository.findAllWithProfileByClubClubId(emptyClub.getClubId());

            assertThat(result).isEmpty();
        }
    }

    @Nested
    class findByProfileProfileIdsWithoutClub_테스트 {

        @Test
        void 동아리_미소속_프로필_ID만_반환한다() {
            List<Long> result = clubMembersRepository.findByProfileProfileIdsWithoutClub(
                    List.of(savedProfile.getProfileId(), secondProfile.getProfileId(), profileWithoutClub.getProfileId())
            );

            assertThat(result).containsExactly(profileWithoutClub.getProfileId());
        }

        @Test
        void 모두_동아리에_소속되어_있으면_빈_리스트를_반환한다() {
            List<Long> result = clubMembersRepository.findByProfileProfileIdsWithoutClub(
                    List.of(savedProfile.getProfileId(), secondProfile.getProfileId())
            );

            assertThat(result).isEmpty();
        }
    }
}
