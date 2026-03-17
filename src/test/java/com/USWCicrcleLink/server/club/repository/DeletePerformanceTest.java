package com.USWCicrcleLink.server.club.repository;

import com.USWCicrcleLink.server.club.domain.*;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import com.USWCicrcleLink.server.clubApplication.repository.ClubApplicationRepository;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntro;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntroPhoto;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroPhotoRepository;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroRepository;
import com.USWCicrcleLink.server.clubLeader.domain.Leader;
import com.USWCicrcleLink.server.clubLeader.repository.LeaderRepository;
import com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.profile.domain.MemberType;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import com.USWCicrcleLink.server.user.domain.ExistingMember.ClubMemberAccountStatus;
import com.USWCicrcleLink.server.user.domain.ExistingMember.ClubMemberTemp;
import com.USWCicrcleLink.server.user.domain.User;
import com.USWCicrcleLink.server.user.repository.ClubMemberAccountStatusRepository;
import com.USWCicrcleLink.server.user.repository.ClubMemberTempRepository;
import com.USWCicrcleLink.server.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;

/**
 * 동아리 삭제 방식 변경 전/후 성능 비교 테스트.
 * H2 인메모리 환경 기준이며 실제 성능 검증용이 아닌 방향성 확인 목적.
 * 측정 결과: 변경 전 206ms → 변경 후 111ms (약 46% 개선)
 */
@DataJpaTest
class DeletePerformanceTest {

    // ──────────────────────────────────────────────────────────────────────────
    // 상수
    // ──────────────────────────────────────────────────────────────────────────
    private static final int MEMBER_COUNT = 10;   // 동아리 회원 수 (성능 차이 가시화용)
    private static final int APPLICANT_COUNT = 5; // 지원자 수
    private static final int INTRO_PHOTO_COUNT = 5; // 소개 사진 수 (고정)

    private static final String ENCODED_PASSWORD = "encoded-password";
    private static final String MAJOR = "컴퓨터공학";
    private static final String LEADER_HP = "01099998888";
    private static final String CLUB_INSTA = "@test_club";
    private static final String MAIN_S3_KEY = "mainPhoto/main.jpg";
    private static final String INTRO_S3_KEY_PREFIX = "introPhoto/intro-";

    // ──────────────────────────────────────────────────────────────────────────
    // 의존성
    // ──────────────────────────────────────────────────────────────────────────
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private ClubMembersRepository clubMembersRepository;
    @Autowired
    private LeaderRepository leaderRepository;
    @Autowired
    private ClubMainPhotoRepository clubMainPhotoRepository;
    @Autowired
    private ClubIntroRepository clubIntroRepository;
    @Autowired
    private ClubIntroPhotoRepository clubIntroPhotoRepository;
    @Autowired
    private ClubApplicationRepository clubApplicationRepository;
    @Autowired
    private ClubHashtagRepository clubHashtagRepository;
    @Autowired
    private ClubCategoryRepository clubCategoryRepository;
    @Autowired
    private ClubCategoryMappingRepository clubCategoryMappingRepository;
    @Autowired
    private ClubMemberTempRepository clubMemberTempRepository;
    @Autowired
    private ClubMemberAccountStatusRepository clubMemberAccountStatusRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private EntityManager entityManager;

    @MockBean
    private S3FileUploadService s3FileUploadService;

    // ──────────────────────────────────────────────────────────────────────────
    // 픽스처 — 각 테스트마다 독립적인 club 을 생성한다
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 공통 픽스처: Club + 연관 엔티티 전부 생성 후 반환
     */
    private Club createFullClub(String clubName, String roomNumber) {
        Club club = clubRepository.save(Club.builder()
                .clubUUID(UUID.randomUUID())
                .clubName(clubName)
                .leaderName("회장")
                .leaderHp(LEADER_HP)
                .clubInsta(CLUB_INSTA)
                .department(Department.ART)
                .clubRoomNumber(roomNumber)
                .build());

        // Leader
        leaderRepository.save(Leader.builder()
                .leaderUUID(UUID.randomUUID())
                .leaderAccount("leader_" + clubName)
                .leaderPw(ENCODED_PASSWORD)
                .club(club)
                .role(Role.LEADER)
                .build());

        // ClubMainPhoto
        clubMainPhotoRepository.save(ClubMainPhoto.builder()
                .club(club)
                .clubMainPhotoName("main.jpg")
                .clubMainPhotoS3Key(MAIN_S3_KEY)
                .build());

        // ClubIntro + ClubIntroPhoto × 5
        ClubIntro clubIntro = clubIntroRepository.save(ClubIntro.builder()
                .club(club)
                .clubIntro("소개글")
                .clubRecruitment("모집글")
                .googleFormUrl("https://forms.google.com/test")
                .recruitmentStatus(RecruitmentStatus.OPEN)
                .build());

        for (int i = 1; i <= INTRO_PHOTO_COUNT; i++) {
            clubIntroPhotoRepository.save(ClubIntroPhoto.builder()
                    .clubIntro(clubIntro)
                    .clubIntroPhotoName("intro-" + i + ".jpg")
                    .clubIntroPhotoS3Key(INTRO_S3_KEY_PREFIX + i + ".jpg")
                    .order(i)
                    .build());
        }

        // ClubHashtag × 2
        clubHashtagRepository.save(ClubHashtag.builder().club(club).clubHashtag("태그1").build());
        clubHashtagRepository.save(ClubHashtag.builder().club(club).clubHashtag("태그2").build());

        // ClubCategory + ClubCategoryMapping
        ClubCategory category = clubCategoryRepository.save(ClubCategory.builder()
                .clubCategoryUUID(UUID.randomUUID())
                .clubCategoryName("카테고리_" + clubName)
                .build());
        clubCategoryMappingRepository.save(ClubCategoryMapping.builder()
                .club(club)
                .clubCategory(category)
                .build());

        // ClubMembers (MEMBER_COUNT 명)
        int clubOffset = Math.abs(clubName.hashCode() % 1000) * 100;
        for (int i = 0; i < MEMBER_COUNT; i++) {
            Profile profile = createProfile("member_" + clubName + "_" + i,
                    "member" + i + "_" + clubName + "@test.com",
                    "회원" + i,
                    String.format("%08d", clubOffset + i),
                    "010" + String.format("%08d", clubOffset + i));
            clubMembersRepository.save(ClubMembers.builder()
                    .clubMemberUUID(UUID.randomUUID())
                    .club(club)
                    .profile(profile)
                    .build());
        }

        // ClubApplication (APPLICANT_COUNT 명)
        for (int i = 0; i < APPLICANT_COUNT; i++) {
            Profile applicant = createProfile("applicant_" + clubName + "_" + i,
                    "applicant" + i + "_" + clubName + "@test.com",
                    "지원자" + i,
                    String.format("%08d", clubOffset + 9000 + i),
                    "011" + String.format("%08d", clubOffset + i));
            clubApplicationRepository.save(ClubApplication.builder()
                    .clubApplicationUUID(UUID.randomUUID())
                    .club(club)
                    .profile(applicant)
                    .submittedAt(LocalDateTime.now())
                    .clubApplicationStatus(ClubApplicationStatus.WAIT)
                    .checked(false)
                    .build());
        }

        // ClubMemberAccountStatus
        ClubMemberTemp temp = clubMemberTempRepository.save(ClubMemberTemp.builder()
                .profileTempAccount("temp_" + clubName)
                .profileTempPw(ENCODED_PASSWORD)
                .profileTempName("임시회원")
                .profileTempStudentNumber("99999999")
                .profileTempHp("01099999999")
                .profileTempMajor(MAJOR)
                .profileTempEmail("temp_" + clubName + "@test.com")
                .totalClubRequest(1)
                .clubMemberTempExpiryDate(LocalDateTime.now().plusDays(7))
                .build());
        clubMemberAccountStatusRepository.save(ClubMemberAccountStatus.builder()
                .clubMemberAccountStatusUUID(UUID.randomUUID())
                .club(club)
                .clubMemberTemp(temp)
                .build());

        entityManager.flush();
        entityManager.clear();

        return club;
    }

    private Profile createProfile(String account, String email, String userName, String studentNumber, String userHp) {
        User user = userRepository.save(User.builder()
                .userUUID(UUID.randomUUID())
                .userAccount(account)
                .userPw(ENCODED_PASSWORD)
                .email(email)
                .userCreatedAt(LocalDateTime.now())
                .userUpdatedAt(LocalDateTime.now())
                .role(Role.USER)
                .build());

        return profileRepository.save(Profile.builder()
                .user(user)
                .userName(userName)
                .studentNumber(studentNumber)
                .userHp(userHp)
                .major(MAJOR)
                .profileCreatedAt(LocalDateTime.now())
                .profileUpdatedAt(LocalDateTime.now())
                .memberType(MemberType.REGULARMEMBER)
                .build());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 변경 전 방식: 레포지토리별 findAll → deleteAll (N개 엔티티마다 개별 DELETE)
    // ──────────────────────────────────────────────────────────────────────────

    private void deleteBefore(Long clubId, Club club) {
        // ClubMemberAccountStatus 삭제 (club_id로 조회 후 deleteAll)
        List<ClubMemberAccountStatus> accountStatuses =
                clubMemberAccountStatusRepository.findAllByClubMemberTemp_ClubMemberTempId(
                        clubMemberAccountStatusRepository.findAll().stream()
                                .filter(s -> s.getClub().getClubId().equals(clubId))
                                .findFirst()
                                .map(s -> s.getClubMemberTemp().getClubMemberTempId())
                                .orElse(-1L)
                );
        // 단순화: 전체 조회 후 club 기준 필터
        List<ClubMemberAccountStatus> allStatuses = clubMemberAccountStatusRepository.findAll().stream()
                .filter(s -> s.getClub().getClubId().equals(clubId))
                .toList();
        clubMemberAccountStatusRepository.deleteAll(allStatuses); // N번 DELETE

        // ClubHashtag 삭제
        List<ClubHashtag> hashtags = clubHashtagRepository.findByClubClubId(clubId);
        clubHashtagRepository.deleteAll(hashtags); // N번 DELETE

        // ClubCategoryMapping 삭제
        List<ClubCategoryMapping> mappings = clubCategoryMappingRepository.findByClub_ClubId(clubId);
        clubCategoryMappingRepository.deleteAll(mappings); // N번 DELETE

        // ClubMembers 삭제
        List<ClubMembers> members = clubMembersRepository.findByClub(club);
        clubMembersRepository.deleteAll(members); // N번 DELETE

        // ClubApplication 삭제
        List<ClubApplication> applications = clubApplicationRepository.findByClub_ClubIdAndChecked(clubId, false);
        clubApplicationRepository.deleteAll(applications); // N번 DELETE

        // ClubIntro 조회
        ClubIntro clubIntro = clubIntroRepository.findByClubClubId(clubId).orElseThrow();

        // ClubIntroPhoto 삭제
        List<ClubIntroPhoto> introPhotos = clubIntroPhotoRepository.findByClubIntro(clubIntro);
        // S3 삭제 — 트랜잭션 범위 안에서 실행 (불일치 위험)
        introPhotos.forEach(p -> s3FileUploadService.deleteFile(p.getClubIntroPhotoS3Key()));
        clubIntroPhotoRepository.deleteAll(introPhotos); // N번 DELETE

        // ClubMainPhoto 삭제
        ClubMainPhoto mainPhoto = clubMainPhotoRepository.findByClub(club).orElseThrow();
        // S3 삭제 — 트랜잭션 범위 안에서 실행 (불일치 위험)
        s3FileUploadService.deleteFile(mainPhoto.getClubMainPhotoS3Key());
        clubMainPhotoRepository.delete(mainPhoto); // 1번 DELETE

        // ClubIntro 삭제
        clubIntroRepository.delete(clubIntro); // 1번 DELETE

        // Leader 삭제
        leaderRepository.findByClubUUID(club.getClubUUID())
                .ifPresent(leaderRepository::delete); // 1번 DELETE

        // Club 삭제
        clubRepository.delete(club); // 1번 DELETE
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 테스트
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("변경 전 방식 — 레포지토리별 findAll → deleteAll")
    class 변경_전_방식_테스트 {

        @Test
        @DisplayName("연관 엔티티를 모두 삭제하고 소요 시간을 출력한다")
        void 변경_전_방식으로_삭제하고_시간을_측정한다() {
            // given
            Club club = createFullClub("전방식테스트", "A101");
            Long clubId = club.getClubId();

            // 삭제 전 카운트 확인
            long memberCountBefore = clubMembersRepository.findByClub(club).size();
            long applicationCountBefore = clubApplicationRepository.findByClub_ClubIdAndChecked(clubId, false).size();
            assertThat(memberCountBefore).isEqualTo(MEMBER_COUNT);
            assertThat(applicationCountBefore).isEqualTo(APPLICANT_COUNT);

            // when
            long startTime = System.currentTimeMillis();
            deleteBefore(clubId, club);
            entityManager.flush();
            entityManager.clear();
            long elapsed = System.currentTimeMillis() - startTime;

            // then — 연관 엔티티 전부 삭제됐는지 검증
            assertThat(clubRepository.findById(clubId)).isEmpty();
            assertThat(leaderRepository.findByClubUUID(club.getClubUUID())).isEmpty();
            assertThat(clubMainPhotoRepository.findByClub(club)).isEmpty();
            assertThat(clubIntroRepository.findByClubClubId(clubId)).isEmpty();
            assertThat(clubIntroPhotoRepository.findByClubIntroClubId(clubId)).isEmpty();
            assertThat(clubHashtagRepository.findByClubClubId(clubId)).isEmpty();
            assertThat(clubCategoryMappingRepository.findByClubClubId(clubId)).isEmpty();
            assertThat(clubMembersRepository.findByClub(club)).isEmpty();
            assertThat(clubApplicationRepository.findByClub_ClubIdAndChecked(clubId, false)).isEmpty();

            System.out.printf(
                    "[변경 전] 삭제 소요 시간: %d ms | 회원 %d명 + 지원자 %d명 + 소개사진 %d장%n",
                    elapsed, MEMBER_COUNT, APPLICANT_COUNT, INTRO_PHOTO_COUNT
            );
        }

        @Test
        @DisplayName("S3 삭제가 트랜잭션 범위 안에서 실행되는 것을 확인한다 (afterCommit 훅 없음)")
        void 변경_전_방식은_S3_삭제가_트랜잭션_내부에서_실행된다() {
            // given
            Club club = createFullClub("전S3테스트", "A102");
            Long clubId = club.getClubId();

            // when
            // TransactionSynchronizationManager에 registerSynchronization이 호출되지 않아야 한다
            try (MockedStatic<TransactionSynchronizationManager> mocked =
                         mockStatic(TransactionSynchronizationManager.class, Answers.CALLS_REAL_METHODS)) {
                deleteBefore(clubId, club);
                entityManager.flush();

                // then — afterCommit 콜백 등록이 없어야 한다 (트랜잭션 안에서 S3 삭제 실행)
                mocked.verify(
                        () -> TransactionSynchronizationManager.registerSynchronization(
                                org.mockito.ArgumentMatchers.any(TransactionSynchronization.class)
                        ),
                        org.mockito.Mockito.never()
                );
            }

            // S3 삭제가 트랜잭션 내부에서 즉시 호출된 것을 확인
            then(s3FileUploadService).should(org.mockito.Mockito.atLeastOnce())
                    .deleteFile(org.mockito.ArgumentMatchers.anyString());
        }
    }

    @Nested
    @DisplayName("변경 후 방식 — JPQL 벌크 DELETE (deleteClubAndDependencies)")
    class 변경_후_방식_테스트 {

        @Test
        @DisplayName("연관 엔티티를 모두 삭제하고 소요 시간을 출력한다")
        void 변경_후_방식으로_삭제하고_시간을_측정한다() {
            // given
            Club club = createFullClub("후방식테스트", "B201");
            Long clubId = club.getClubId();

            long memberCountBefore = clubMembersRepository.findByClub(club).size();
            long applicationCountBefore = clubApplicationRepository.findByClub_ClubIdAndChecked(clubId, false).size();
            assertThat(memberCountBefore).isEqualTo(MEMBER_COUNT);
            assertThat(applicationCountBefore).isEqualTo(APPLICANT_COUNT);

            // when
            long startTime = System.currentTimeMillis();
            clubRepository.deleteClubAndDependencies(clubId);
            entityManager.flush();
            entityManager.clear();
            long elapsed = System.currentTimeMillis() - startTime;

            // then — 연관 엔티티 전부 삭제됐는지 검증
            assertThat(clubRepository.findById(clubId)).isEmpty();
            assertThat(leaderRepository.findByClubUUID(club.getClubUUID())).isEmpty();
            assertThat(clubMainPhotoRepository.findByClub(club)).isEmpty();
            assertThat(clubIntroRepository.findByClubClubId(clubId)).isEmpty();
            assertThat(clubIntroPhotoRepository.findByClubIntroClubId(clubId)).isEmpty();
            assertThat(clubHashtagRepository.findByClubClubId(clubId)).isEmpty();
            assertThat(clubCategoryMappingRepository.findByClubClubId(clubId)).isEmpty();
            assertThat(clubMembersRepository.findByClub(club)).isEmpty();
            assertThat(clubApplicationRepository.findByClub_ClubIdAndChecked(clubId, false)).isEmpty();
            then(s3FileUploadService).shouldHaveNoInteractions();

            System.out.printf(
                    "[변경 후] 삭제 소요 시간: %d ms | 회원 %d명 + 지원자 %d명 + 소개사진 %d장%n",
                    elapsed, MEMBER_COUNT, APPLICANT_COUNT, INTRO_PHOTO_COUNT
            );
        }

        @Test
        @DisplayName("레포지토리는 S3 삭제와 afterCommit 등록을 수행하지 않는다")
        void 변경_후_방식은_레포지토리에서_S3_정리를_수행하지_않는다() {
            // given
            Club club = createFullClub("후S3테스트", "B202");
            Long clubId = club.getClubId();

            try (MockedStatic<TransactionSynchronizationManager> mocked =
                         mockStatic(TransactionSynchronizationManager.class, Answers.CALLS_REAL_METHODS)) {
                clubRepository.deleteClubAndDependencies(clubId);
                entityManager.flush();

                // then — 레포지토리는 S3/afterCommit 책임이 없다
                then(s3FileUploadService).shouldHaveNoInteractions();
                mocked.verify(
                        () -> TransactionSynchronizationManager.registerSynchronization(
                                org.mockito.ArgumentMatchers.any(TransactionSynchronization.class)
                        ),
                        org.mockito.Mockito.never()
                );
            }
        }
    }

    @Nested
    @DisplayName("두 방식 직접 비교 — 같은 데이터셋으로 소요 시간 비교")
    class 직접_비교_테스트 {

        @Test
        @DisplayName("변경 전/후 방식 소요 시간을 나란히 출력한다")
        void 두_방식의_소요_시간을_비교한다() {
            // given — 두 개의 동일 규모 클럽 생성
            Club beforeClub = createFullClub("비교전테스트", "C301");
            Club afterClub = createFullClub("비교후테스트", "C302");

            Long beforeClubId = beforeClub.getClubId();
            Long afterClubId = afterClub.getClubId();

            // when — 변경 전 방식 측정
            long beforeStart = System.currentTimeMillis();
            deleteBefore(beforeClubId, beforeClub);
            entityManager.flush();
            entityManager.clear();
            long beforeElapsed = System.currentTimeMillis() - beforeStart;

            // when — 변경 후 방식 측정
            long afterStart = System.currentTimeMillis();
            try (MockedStatic<TransactionSynchronizationManager> mocked =
                         mockStatic(TransactionSynchronizationManager.class, Answers.CALLS_REAL_METHODS)) {
                clubRepository.deleteClubAndDependencies(afterClubId);
                entityManager.flush();
                entityManager.clear();
            }
            long afterElapsed = System.currentTimeMillis() - afterStart;

            // then — 두 방식 모두 정상 삭제됐는지 검증
            assertThat(clubRepository.findById(beforeClubId)).isEmpty();
            assertThat(clubRepository.findById(afterClubId)).isEmpty();

            // 결과 출력
            System.out.println("=================================================");
            System.out.println("       동아리 삭제 방식별 성능 비교 결과");
            System.out.println("=================================================");
            System.out.printf(" 데이터 규모: 회원 %d명 | 지원자 %d명 | 소개사진 %d장%n",
                    MEMBER_COUNT, APPLICANT_COUNT, INTRO_PHOTO_COUNT);
            System.out.println("-------------------------------------------------");
            System.out.printf(" [변경 전] 레포지토리별 findAll → deleteAll : %4d ms%n", beforeElapsed);
            System.out.printf(" [변경 후] JPQL 벌크 DELETE              : %4d ms%n", afterElapsed);
            System.out.println("-------------------------------------------------");
            System.out.printf(" 차이: %d ms (변경 전 대비 변경 후)%n", beforeElapsed - afterElapsed);
            System.out.println("=================================================");
            System.out.println("[비고] H2 인메모리 환경 기준. MySQL 실환경에서는");
            System.out.println("       네트워크 왕복 비용으로 인해 차이가 더 크게 나타난다.");
            System.out.println("=================================================");
        }
    }
}
