package com.USWCicrcleLink.server.club.service;

import com.USWCicrcleLink.server.admin.admin.dto.AdminClubIntroResponse;
import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.ClubCategory;
import com.USWCicrcleLink.server.club.domain.ClubCategoryMapping;
import com.USWCicrcleLink.server.club.domain.ClubHashtag;
import com.USWCicrcleLink.server.club.domain.ClubMainPhoto;
import com.USWCicrcleLink.server.club.domain.Department;
import com.USWCicrcleLink.server.club.domain.RecruitmentStatus;
import com.USWCicrcleLink.server.club.dto.ClubCategoryResponse;
import com.USWCicrcleLink.server.club.dto.ClubInfoListResponse;
import com.USWCicrcleLink.server.club.dto.ClubListByClubCategoryResponse;
import com.USWCicrcleLink.server.club.dto.ClubListResponse;
import com.USWCicrcleLink.server.club.repository.ClubCategoryMappingRepository;
import com.USWCicrcleLink.server.club.repository.ClubCategoryRepository;
import com.USWCicrcleLink.server.club.repository.ClubHashtagRepository;
import com.USWCicrcleLink.server.club.repository.ClubMainPhotoRepository;
import com.USWCicrcleLink.server.club.repository.ClubRepository;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntro;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntroPhoto;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroPhotoRepository;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.BaseException;
import com.USWCicrcleLink.server.global.exception.errortype.ClubException;
import com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ClubServiceTest {

    private static final String CLUB_NAME = "밴드";
    private static final String SECOND_CLUB_NAME = "축구";
    private static final String MAIN_S3_KEY = "club/main.jpg";
    private static final String INTRO_S3_KEY = "club/intro-1.jpg";
    private static final String SECOND_INTRO_S3_KEY = "club/intro-2.jpg";
    private static final String MAIN_PHOTO_URL = "https://presigned.example.com/main";
    private static final String INTRO_PHOTO_URL = "https://presigned.example.com/intro-1";
    private static final String SECOND_INTRO_PHOTO_URL = "https://presigned.example.com/intro-2";
    private static final String HASHTAG = "공연";
    private static final String SECOND_HASHTAG = "친목";
    private static final String CATEGORY_NAME = "예술";
    private static final String SECOND_CATEGORY_NAME = "체육";

    @Mock private ClubCategoryMappingRepository clubCategoryMappingRepository;
    @Mock private ClubCategoryRepository clubCategoryRepository;
    @Mock private ClubMainPhotoRepository clubMainPhotoRepository;
    @Mock private ClubHashtagRepository clubHashtagRepository;
    @Mock private S3FileUploadService s3FileUploadService;
    @Mock private ClubIntroRepository clubIntroRepository;
    @Mock private ClubRepository clubRepository;
    @Mock private ClubIntroPhotoRepository clubIntroPhotoRepository;

    @InjectMocks
    private ClubService clubService;

    private Club club;
    private Club secondClub;
    private UUID clubUUID;
    private UUID secondClubUUID;
    private UUID categoryUUID;
    private UUID secondCategoryUUID;
    private ClubCategory clubCategory;
    private ClubCategory secondClubCategory;

    @BeforeEach
    void setUp() {
        clubUUID = UUID.randomUUID();
        secondClubUUID = UUID.randomUUID();
        categoryUUID = UUID.randomUUID();
        secondCategoryUUID = UUID.randomUUID();

        club = Club.builder()
                .clubId(1L)
                .clubUUID(clubUUID)
                .clubName(CLUB_NAME)
                .leaderName("홍길동")
                .leaderHp("01012345678")
                .clubInsta("@band")
                .department(Department.ART)
                .clubRoomNumber("A101")
                .build();
        secondClub = Club.builder()
                .clubId(2L)
                .clubUUID(secondClubUUID)
                .clubName(SECOND_CLUB_NAME)
                .leaderName("김철수")
                .leaderHp("01098765432")
                .clubInsta("@soccer")
                .department(Department.SPORT)
                .clubRoomNumber("B202")
                .build();
        clubCategory = ClubCategory.builder()
                .clubCategoryId(10L)
                .clubCategoryUUID(categoryUUID)
                .clubCategoryName(CATEGORY_NAME)
                .build();
        secondClubCategory = ClubCategory.builder()
                .clubCategoryId(20L)
                .clubCategoryUUID(secondCategoryUUID)
                .clubCategoryName(SECOND_CATEGORY_NAME)
                .build();
    }

    @Nested
    class getAllClubs_테스트 {

        @Test
        void 전체_동아리_목록을_정상_반환한다() {
            given(clubRepository.findAll()).willReturn(List.of(club, secondClub));
            given(clubMainPhotoRepository.findByClubIds(List.of(1L, 2L)))
                    .willReturn(List.of(
                            ClubMainPhoto.builder().club(club).clubMainPhotoS3Key(MAIN_S3_KEY).build()
                    ));
            given(clubHashtagRepository.findByClubIds(List.of(1L, 2L)))
                    .willReturn(List.of(
                            ClubHashtag.builder().club(club).clubHashtag(HASHTAG).build(),
                            ClubHashtag.builder().club(club).clubHashtag(SECOND_HASHTAG).build()
                    ));
            given(s3FileUploadService.generatePresignedGetUrl(MAIN_S3_KEY)).willReturn(MAIN_PHOTO_URL);

            List<ClubListResponse> result = clubService.getAllClubs();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getClubUUID()).isEqualTo(clubUUID);
            assertThat(result.get(0).getMainPhoto()).isEqualTo(MAIN_PHOTO_URL);
            assertThat(result.get(0).getDepartmentName()).isEqualTo(Department.ART.name());
            assertThat(result.get(0).getClubHashtags()).containsExactly(HASHTAG, SECOND_HASHTAG);
            assertThat(result.get(1).getMainPhoto()).isNull();
            assertThat(result.get(1).getClubHashtags()).isEmpty();
        }
    }

    @Nested
    class getAllClubsInfo_테스트 {

        @Test
        void 전체_동아리_정보를_메인_사진과_함께_반환한다() {
            given(clubRepository.findAll()).willReturn(List.of(club, secondClub));
            given(clubMainPhotoRepository.findByClub(club))
                    .willReturn(Optional.of(ClubMainPhoto.builder().club(club).clubMainPhotoS3Key(MAIN_S3_KEY).build()));
            given(clubMainPhotoRepository.findByClub(secondClub)).willReturn(Optional.empty());
            given(s3FileUploadService.generatePresignedGetUrl(MAIN_S3_KEY)).willReturn(MAIN_PHOTO_URL);

            List<ClubInfoListResponse> result = clubService.getAllClubsInfo();

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getClubUUID()).isEqualTo(clubUUID);
            assertThat(result.get(0).getMainPhoto()).isEqualTo(MAIN_PHOTO_URL);
            assertThat(result.get(1).getClubUUID()).isEqualTo(secondClubUUID);
            assertThat(result.get(1).getMainPhoto()).isNull();
        }
    }

    @Nested
    class getAllClubsByClubCategories_테스트 {

        @Test
        void 카테고리별_동아리_목록을_정상_반환한다() {
            List<UUID> clubCategoryUUIDs = List.of(categoryUUID);
            given(clubCategoryRepository.findClubCategoryIdsByUUIDs(clubCategoryUUIDs)).willReturn(List.of(10L));
            given(clubCategoryMappingRepository.findClubsByCategoryIds(List.of(10L))).willReturn(List.of(club));
            given(clubMainPhotoRepository.findByClubIds(List.of(1L)))
                    .willReturn(List.of(ClubMainPhoto.builder().club(club).clubMainPhotoS3Key(MAIN_S3_KEY).build()));
            given(clubHashtagRepository.findByClubIds(List.of(1L)))
                    .willReturn(List.of(ClubHashtag.builder().club(club).clubHashtag(HASHTAG).build()));
            given(s3FileUploadService.generatePresignedGetUrl(MAIN_S3_KEY)).willReturn(MAIN_PHOTO_URL);
            given(clubCategoryRepository.findById(10L)).willReturn(Optional.of(clubCategory));

            List<ClubListByClubCategoryResponse> result = clubService.getAllClubsByClubCategories(clubCategoryUUIDs);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getClubCategoryUUID()).isEqualTo(categoryUUID);
            assertThat(result.get(0).getClubCategoryName()).isEqualTo(CATEGORY_NAME);
            assertThat(result.get(0).getClubs()).hasSize(1);
            assertThat(result.get(0).getClubs().get(0).getClubUUID()).isEqualTo(clubUUID);
        }

        @Test
        void 카테고리가_3개를_초과하면_INVALID_CATEGORY_COUNT_예외가_발생한다() {
            List<UUID> clubCategoryUUIDs = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            assertThatThrownBy(() -> clubService.getAllClubsByClubCategories(clubCategoryUUIDs))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getExceptionType())
                    .isEqualTo(ExceptionType.INVALID_CATEGORY_COUNT);
        }

        @Test
        void 카테고리_ID를_찾지_못하면_CATEGORY_NOT_FOUND_예외가_발생한다() {
            List<UUID> clubCategoryUUIDs = List.of(categoryUUID);
            given(clubCategoryRepository.findClubCategoryIdsByUUIDs(clubCategoryUUIDs)).willReturn(List.of());

            assertThatThrownBy(() -> clubService.getAllClubsByClubCategories(clubCategoryUUIDs))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getExceptionType())
                    .isEqualTo(ExceptionType.CATEGORY_NOT_FOUND);
        }

        @Test
        void 카테고리_엔티티를_찾지_못하면_CATEGORY_NOT_FOUND_예외가_발생한다() {
            List<UUID> clubCategoryUUIDs = List.of(categoryUUID);
            given(clubCategoryRepository.findClubCategoryIdsByUUIDs(clubCategoryUUIDs)).willReturn(List.of(10L));
            given(clubCategoryMappingRepository.findClubsByCategoryIds(List.of(10L))).willReturn(List.of(club));
            given(clubMainPhotoRepository.findByClubIds(List.of(1L))).willReturn(List.of());
            given(clubHashtagRepository.findByClubIds(List.of(1L))).willReturn(List.of());
            given(clubCategoryRepository.findById(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clubService.getAllClubsByClubCategories(clubCategoryUUIDs))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getExceptionType())
                    .isEqualTo(ExceptionType.CATEGORY_NOT_FOUND);
        }
    }

    @Nested
    class getOpenClubs_테스트 {

        @Test
        void 모집중인_동아리만_정상_반환한다() {
            given(clubIntroRepository.findOpenClubIds()).willReturn(List.of(1L));
            given(clubRepository.findByClubIds(List.of(1L))).willReturn(List.of(club));
            given(clubMainPhotoRepository.findByClubIds(List.of(1L)))
                    .willReturn(List.of(ClubMainPhoto.builder().club(club).clubMainPhotoS3Key(MAIN_S3_KEY).build()));
            given(clubHashtagRepository.findByClubIds(List.of(1L)))
                    .willReturn(List.of(ClubHashtag.builder().club(club).clubHashtag(HASHTAG).build()));
            given(s3FileUploadService.generatePresignedGetUrl(MAIN_S3_KEY)).willReturn(MAIN_PHOTO_URL);

            List<ClubListResponse> result = clubService.getOpenClubs();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getClubUUID()).isEqualTo(clubUUID);
            assertThat(result.get(0).getMainPhoto()).isEqualTo(MAIN_PHOTO_URL);
            assertThat(result.get(0).getClubHashtags()).containsExactly(HASHTAG);
        }
    }

    @Nested
    class getOpenClubsByClubCategories_테스트 {

        @Test
        void 모집중인_카테고리별_동아리_목록을_정상_반환한다() {
            List<UUID> clubCategoryUUIDs = List.of(categoryUUID);
            given(clubCategoryRepository.findClubCategoryIdsByUUIDs(clubCategoryUUIDs)).willReturn(List.of(10L));
            given(clubIntroRepository.findOpenClubIds()).willReturn(List.of(1L));
            given(clubCategoryMappingRepository.findOpenClubsByCategoryIds(List.of(10L), List.of(1L)))
                    .willReturn(List.of(club));
            given(clubMainPhotoRepository.findByClubIds(List.of(1L)))
                    .willReturn(List.of(ClubMainPhoto.builder().club(club).clubMainPhotoS3Key(MAIN_S3_KEY).build()));
            given(clubHashtagRepository.findByClubIds(List.of(1L)))
                    .willReturn(List.of(ClubHashtag.builder().club(club).clubHashtag(HASHTAG).build()));
            given(s3FileUploadService.generatePresignedGetUrl(MAIN_S3_KEY)).willReturn(MAIN_PHOTO_URL);
            given(clubCategoryRepository.findById(10L)).willReturn(Optional.of(clubCategory));

            List<ClubListByClubCategoryResponse> result = clubService.getOpenClubsByClubCategories(clubCategoryUUIDs);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getClubCategoryUUID()).isEqualTo(categoryUUID);
            assertThat(result.get(0).getClubs()).hasSize(1);
            assertThat(result.get(0).getClubs().get(0).getClubUUID()).isEqualTo(clubUUID);
        }

        @Test
        void 카테고리가_3개를_초과하면_INVALID_CATEGORY_COUNT_예외가_발생한다() {
            List<UUID> clubCategoryUUIDs = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

            assertThatThrownBy(() -> clubService.getOpenClubsByClubCategories(clubCategoryUUIDs))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getExceptionType())
                    .isEqualTo(ExceptionType.INVALID_CATEGORY_COUNT);
        }

        @Test
        void 카테고리_ID를_찾지_못하면_CATEGORY_NOT_FOUND_예외가_발생한다() {
            List<UUID> clubCategoryUUIDs = List.of(categoryUUID);
            given(clubCategoryRepository.findClubCategoryIdsByUUIDs(clubCategoryUUIDs)).willReturn(List.of());

            assertThatThrownBy(() -> clubService.getOpenClubsByClubCategories(clubCategoryUUIDs))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getExceptionType())
                    .isEqualTo(ExceptionType.CATEGORY_NOT_FOUND);
        }

        @Test
        void 카테고리_엔티티를_찾지_못하면_CATEGORY_NOT_FOUND_예외가_발생한다() {
            List<UUID> clubCategoryUUIDs = List.of(categoryUUID);
            given(clubCategoryRepository.findClubCategoryIdsByUUIDs(clubCategoryUUIDs)).willReturn(List.of(10L));
            given(clubIntroRepository.findOpenClubIds()).willReturn(List.of(1L));
            given(clubCategoryMappingRepository.findOpenClubsByCategoryIds(List.of(10L), List.of(1L)))
                    .willReturn(List.of(club));
            given(clubMainPhotoRepository.findByClubIds(List.of(1L))).willReturn(List.of());
            given(clubHashtagRepository.findByClubIds(List.of(1L))).willReturn(List.of());
            given(clubCategoryRepository.findById(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clubService.getOpenClubsByClubCategories(clubCategoryUUIDs))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getExceptionType())
                    .isEqualTo(ExceptionType.CATEGORY_NOT_FOUND);
        }
    }

    @Nested
    class getAllClubCategories_테스트 {

        @Test
        void 전체_카테고리를_정상_반환한다() {
            given(clubCategoryRepository.findAll()).willReturn(List.of(clubCategory, secondClubCategory));

            List<ClubCategoryResponse> result = clubService.getAllClubCategories();

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(ClubCategoryResponse::getClubCategoryUUID)
                    .containsExactly(categoryUUID, secondCategoryUUID);
            assertThat(result)
                    .extracting(ClubCategoryResponse::getClubCategoryName)
                    .containsExactly(CATEGORY_NAME, SECOND_CATEGORY_NAME);
        }
    }

    @Nested
    class getClubIntro_테스트 {

        @Test
        void 동아리_소개를_정상_반환한다() {
            ClubIntro clubIntro = ClubIntro.builder()
                    .club(club)
                    .clubIntro("소개")
                    .clubRecruitment("모집")
                    .googleFormUrl("https://forms.google.com/test")
                    .recruitmentStatus(RecruitmentStatus.OPEN)
                    .build();
            ClubCategoryMapping categoryMapping = ClubCategoryMapping.builder()
                    .club(club)
                    .clubCategory(clubCategory)
                    .build();
            given(clubRepository.findByClubUUID(clubUUID)).willReturn(Optional.of(club));
            given(clubIntroRepository.findByClubClubId(1L)).willReturn(Optional.of(clubIntro));
            given(clubMainPhotoRepository.findByClubClubId(1L))
                    .willReturn(Optional.of(ClubMainPhoto.builder().club(club).clubMainPhotoS3Key(MAIN_S3_KEY).build()));
            given(clubIntroPhotoRepository.findByClubIntroClubId(1L))
                    .willReturn(List.of(
                            ClubIntroPhoto.builder().clubIntro(clubIntro).clubIntroPhotoS3Key(SECOND_INTRO_S3_KEY).order(2).build(),
                            ClubIntroPhoto.builder().clubIntro(clubIntro).clubIntroPhotoS3Key(INTRO_S3_KEY).order(1).build()
                    ));
            given(clubHashtagRepository.findByClubClubId(1L))
                    .willReturn(List.of(
                            ClubHashtag.builder().club(club).clubHashtag(HASHTAG).build(),
                            ClubHashtag.builder().club(club).clubHashtag(SECOND_HASHTAG).build()
                    ));
            given(clubCategoryMappingRepository.findByClubClubId(1L)).willReturn(List.of(categoryMapping));
            given(s3FileUploadService.generatePresignedGetUrl(MAIN_S3_KEY)).willReturn(MAIN_PHOTO_URL);
            given(s3FileUploadService.generatePresignedGetUrl(INTRO_S3_KEY)).willReturn(INTRO_PHOTO_URL);
            given(s3FileUploadService.generatePresignedGetUrl(SECOND_INTRO_S3_KEY)).willReturn(SECOND_INTRO_PHOTO_URL);

            AdminClubIntroResponse result = clubService.getClubIntro(clubUUID);

            assertThat(result.getClubUUID()).isEqualTo(clubUUID);
            assertThat(result.getMainPhoto()).isEqualTo(MAIN_PHOTO_URL);
            assertThat(result.getIntroPhotos()).containsExactly(INTRO_PHOTO_URL, SECOND_INTRO_PHOTO_URL);
            assertThat(result.getClubName()).isEqualTo(CLUB_NAME);
            assertThat(result.getLeaderName()).isEqualTo("홍길동");
            assertThat(result.getLeaderHp()).isEqualTo("01012345678");
            assertThat(result.getClubInsta()).isEqualTo("@band");
            assertThat(result.getClubIntro()).isEqualTo("소개");
            assertThat(result.getRecruitmentStatus()).isEqualTo(RecruitmentStatus.OPEN);
            assertThat(result.getGoogleFormUrl()).isEqualTo("https://forms.google.com/test");
            assertThat(result.getClubHashtags()).containsExactly(HASHTAG, SECOND_HASHTAG);
            assertThat(result.getClubCategoryNames()).containsExactly(CATEGORY_NAME);
            assertThat(result.getClubRoomNumber()).isEqualTo("A101");
            assertThat(result.getClubRecruitment()).isEqualTo("모집");
        }

        @Test
        void 동아리가_없으면_CLUB_NOT_EXISTS_예외가_발생한다() {
            given(clubRepository.findByClubUUID(clubUUID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clubService.getClubIntro(clubUUID))
                    .isInstanceOf(ClubException.class)
                    .extracting(e -> ((ClubException) e).getExceptionType())
                    .isEqualTo(ExceptionType.CLUB_NOT_EXISTS);
        }

        @Test
        void 동아리_소개가_없으면_CLUB_INTRO_NOT_EXISTS_예외가_발생한다() {
            given(clubRepository.findByClubUUID(clubUUID)).willReturn(Optional.of(club));
            given(clubIntroRepository.findByClubClubId(1L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> clubService.getClubIntro(clubUUID))
                    .isInstanceOf(ClubException.class)
                    .extracting(e -> ((ClubException) e).getExceptionType())
                    .isEqualTo(ExceptionType.CLUB_INTRO_NOT_EXISTS);
        }
    }
}
