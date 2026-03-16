package com.USWCicrcleLink.server.admin.admin.service;

import com.USWCicrcleLink.server.admin.admin.dto.AdminClubCategoryCreationRequest;
import com.USWCicrcleLink.server.club.domain.ClubCategory;
import com.USWCicrcleLink.server.club.dto.ClubCategoryResponse;
import com.USWCicrcleLink.server.club.repository.ClubCategoryMappingRepository;
import com.USWCicrcleLink.server.club.repository.ClubCategoryRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.BaseException;
import com.USWCicrcleLink.server.global.exception.errortype.ClubCategoryException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AdminClubCategoryServiceTest {

    private static final String CATEGORY_NAME = "공연";
    private static final String NORMALIZED_CATEGORY_NAME = "공연";
    private static final String ANOTHER_CATEGORY_NAME = "체육";

    @Mock private ClubCategoryRepository clubCategoryRepository;
    @Mock private ClubCategoryMappingRepository clubCategoryMappingRepository;

    @InjectMocks
    private AdminClubCategoryService adminClubCategoryService;

    private UUID clubCategoryUUID;
    private ClubCategory clubCategory;

    @BeforeEach
    void setUp() {
        clubCategoryUUID = UUID.randomUUID();
        clubCategory = ClubCategory.builder()
                .clubCategoryId(1L)
                .clubCategoryUUID(clubCategoryUUID)
                .clubCategoryName(CATEGORY_NAME)
                .build();
    }

    @Nested
    class getAllClubCategories_테스트 {

        @Test
        void 카테고리_전체_목록을_정상_반환한다() {
            ClubCategory anotherClubCategory = ClubCategory.builder()
                    .clubCategoryId(2L)
                    .clubCategoryUUID(UUID.randomUUID())
                    .clubCategoryName(ANOTHER_CATEGORY_NAME)
                    .build();
            given(clubCategoryRepository.findAll()).willReturn(List.of(clubCategory, anotherClubCategory));

            List<ClubCategoryResponse> result = adminClubCategoryService.getAllClubCategories();

            assertThat(result).hasSize(2);
            assertThat(result)
                    .extracting(ClubCategoryResponse::getClubCategoryUUID)
                    .containsExactly(clubCategoryUUID, anotherClubCategory.getClubCategoryUUID());
            assertThat(result)
                    .extracting(ClubCategoryResponse::getClubCategoryName)
                    .containsExactly(CATEGORY_NAME, ANOTHER_CATEGORY_NAME);
            then(clubCategoryRepository).should().findAll();
        }

        @Test
        void 카테고리가_없으면_빈_목록을_반환한다() {
            given(clubCategoryRepository.findAll()).willReturn(List.of());

            List<ClubCategoryResponse> result = adminClubCategoryService.getAllClubCategories();

            assertThat(result).isEmpty();
            then(clubCategoryRepository).should().findAll();
        }
    }

    @Nested
    class addClubCategory_테스트 {

        @Test
        void 새_카테고리를_추가하면_정규화된_이름으로_반환한다() {
            AdminClubCategoryCreationRequest request = new AdminClubCategoryCreationRequest(CATEGORY_NAME);
            ClubCategory savedClubCategory = ClubCategory.builder()
                    .clubCategoryId(1L)
                    .clubCategoryUUID(clubCategoryUUID)
                    .clubCategoryName(NORMALIZED_CATEGORY_NAME)
                    .build();
            given(clubCategoryRepository.existsByClubCategoryName(NORMALIZED_CATEGORY_NAME)).willReturn(false);
            given(clubCategoryRepository.save(any(ClubCategory.class))).willReturn(savedClubCategory);

            ClubCategoryResponse result = adminClubCategoryService.addClubCategory(request);

            assertThat(result.getClubCategoryUUID()).isEqualTo(clubCategoryUUID);
            assertThat(result.getClubCategoryName()).isEqualTo(NORMALIZED_CATEGORY_NAME);
            then(clubCategoryRepository).should().existsByClubCategoryName(NORMALIZED_CATEGORY_NAME);
            then(clubCategoryRepository).should().save(any(ClubCategory.class));
        }

        @Test
        void 중복된_카테고리명이면_DUPLICATE_CATEGORY_예외가_발생한다() {
            AdminClubCategoryCreationRequest request = new AdminClubCategoryCreationRequest(CATEGORY_NAME);
            given(clubCategoryRepository.existsByClubCategoryName(NORMALIZED_CATEGORY_NAME)).willReturn(true);

            assertThatThrownBy(() -> adminClubCategoryService.addClubCategory(request))
                    .isInstanceOf(ClubCategoryException.class)
                    .extracting(e -> ((ClubCategoryException) e).getExceptionType())
                    .isEqualTo(ExceptionType.DUPLICATE_CATEGORY);

            then(clubCategoryRepository).should().existsByClubCategoryName(NORMALIZED_CATEGORY_NAME);
            then(clubCategoryRepository).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    class deleteClubCategory_테스트 {

        @Test
        void 카테고리를_정상_삭제하면_삭제된_카테고리_정보를_반환한다() {
            given(clubCategoryRepository.findByClubCategoryUUID(clubCategoryUUID)).willReturn(Optional.of(clubCategory));

            ClubCategoryResponse result = adminClubCategoryService.deleteClubCategory(clubCategoryUUID);

            assertThat(result.getClubCategoryUUID()).isEqualTo(clubCategoryUUID);
            assertThat(result.getClubCategoryName()).isEqualTo(CATEGORY_NAME);
            then(clubCategoryRepository).should().findByClubCategoryUUID(clubCategoryUUID);
            then(clubCategoryMappingRepository).should().deleteByClubCategoryId(1L);
            then(clubCategoryRepository).should().delete(clubCategory);
        }

        @Test
        void 카테고리가_존재하지_않으면_CATEGORY_NOT_FOUND_예외가_발생한다() {
            given(clubCategoryRepository.findByClubCategoryUUID(clubCategoryUUID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminClubCategoryService.deleteClubCategory(clubCategoryUUID))
                    .isInstanceOf(ClubCategoryException.class)
                    .extracting(e -> ((ClubCategoryException) e).getExceptionType())
                    .isEqualTo(ExceptionType.CATEGORY_NOT_FOUND);

            then(clubCategoryRepository).should().findByClubCategoryUUID(clubCategoryUUID);
            then(clubCategoryMappingRepository).shouldHaveNoInteractions();
        }

        @Test
        void 삭제_중_예외가_발생하면_SERVER_ERROR_예외로_변환한다() {
            RuntimeException runtimeException = new RuntimeException("delete failed");
            given(clubCategoryRepository.findByClubCategoryUUID(clubCategoryUUID)).willReturn(Optional.of(clubCategory));
            doThrow(runtimeException).when(clubCategoryMappingRepository).deleteByClubCategoryId(1L);

            assertThatThrownBy(() -> adminClubCategoryService.deleteClubCategory(clubCategoryUUID))
                    .isInstanceOf(BaseException.class)
                    .extracting(e -> ((BaseException) e).getExceptionType())
                    .isEqualTo(ExceptionType.SERVER_ERROR);

            then(clubCategoryRepository).should().findByClubCategoryUUID(clubCategoryUUID);
            then(clubCategoryMappingRepository).should().deleteByClubCategoryId(1L);
        }
    }
}
