package com.USWCicrcleLink.server.admin.notice.service;

import com.USWCicrcleLink.server.admin.admin.domain.Admin;
import com.USWCicrcleLink.server.admin.notice.domain.Notice;
import com.USWCicrcleLink.server.admin.notice.domain.NoticePhoto;
import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticeCreationRequest;
import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticePageListResponse;
import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticeUpdateRequest;
import com.USWCicrcleLink.server.admin.notice.dto.NoticeDetailResponse;
import com.USWCicrcleLink.server.admin.notice.repository.NoticePhotoRepository;
import com.USWCicrcleLink.server.admin.notice.repository.NoticeRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.NoticeException;
import com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService;
import com.USWCicrcleLink.server.global.s3File.dto.S3FileResponse;
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
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class AdminNoticeServiceTest {

    private static final String ADMIN_NAME = "관리자";
    private static final String NOTICE_TITLE = "제목";
    private static final String NOTICE_CONTENT = "내용";
    private static final String UPDATED_NOTICE_TITLE = "수정 제목";
    private static final String UPDATED_NOTICE_CONTENT = "수정 내용";
    private static final String FIRST_PHOTO_NAME = "notice-1.jpg";
    private static final String SECOND_PHOTO_NAME = "notice-2.jpg";
    private static final String FIRST_S3_KEY = "noticePhoto/notice-1.jpg";
    private static final String SECOND_S3_KEY = "noticePhoto/notice-2.jpg";
    private static final String FIRST_PRESIGNED_URL = "https://presigned.example.com/notice-1";
    private static final String SECOND_PRESIGNED_URL = "https://presigned.example.com/notice-2";
    private static final String OLD_S3_KEY = "noticePhoto/old.jpg";

    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticePhotoRepository noticePhotoRepository;
    @Mock private S3FileUploadService s3FileUploadService;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;
    @Mock private MultipartFile firstPhoto;
    @Mock private MultipartFile secondPhoto;

    @InjectMocks
    private AdminNoticeService adminNoticeService;

    @Captor
    private ArgumentCaptor<List<NoticePhoto>> noticePhotoListCaptor;

    private Admin admin;
    private Notice notice;
    private UUID noticeUUID;
    private LocalDateTime createdAt;

    @BeforeEach
    void setUp() {
        noticeUUID = UUID.randomUUID();
        createdAt = LocalDateTime.of(2026, 3, 17, 12, 0);
        admin = Admin.builder()
                .adminUUID(UUID.randomUUID())
                .adminAccount("admin")
                .adminPw("encoded")
                .adminName(ADMIN_NAME)
                .role(Role.ADMIN)
                .build();
        notice = Notice.builder()
                .noticeId(1L)
                .noticeUUID(noticeUUID)
                .noticeTitle(NOTICE_TITLE)
                .noticeContent(NOTICE_CONTENT)
                .noticeCreatedAt(createdAt)
                .admin(admin)
                .build();
    }

    private void mockAuthentication() {
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(new CustomAdminDetails(admin));
    }

    @Nested
    class getNotices_테스트 {

        @Test
        void 공지사항_페이지를_정상_반환한다() {
            Pageable pageable = PageRequest.of(1, 2);
            Notice secondNotice = Notice.builder()
                    .noticeId(2L)
                    .noticeUUID(UUID.randomUUID())
                    .noticeTitle(UPDATED_NOTICE_TITLE)
                    .noticeContent(UPDATED_NOTICE_CONTENT)
                    .noticeCreatedAt(createdAt.plusDays(1))
                    .admin(admin)
                    .build();
            given(noticeRepository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(notice, secondNotice), pageable, 4));

            AdminNoticePageListResponse result = adminNoticeService.getNotices(pageable);

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent())
                    .extracting(item -> item.getNoticeUUID())
                    .containsExactly(noticeUUID, secondNotice.getNoticeUUID());
            assertThat(result.getContent())
                    .extracting(item -> item.getNoticeTitle())
                    .containsExactly(NOTICE_TITLE, UPDATED_NOTICE_TITLE);
            assertThat(result.getContent())
                    .extracting(item -> item.getAdminName())
                    .containsOnly(ADMIN_NAME);
            assertThat(result.getTotalPages()).isEqualTo(2);
            assertThat(result.getTotalElements()).isEqualTo(4);
            assertThat(result.getCurrentPage()).isEqualTo(1);
            then(noticeRepository).should().findAll(pageable);
        }

        @Test
        void 공지사항이_없으면_빈_페이지를_반환한다() {
            Pageable pageable = PageRequest.of(0, 10);
            given(noticeRepository.findAll(pageable))
                    .willReturn(new PageImpl<>(List.of(), pageable, 0));

            AdminNoticePageListResponse result = adminNoticeService.getNotices(pageable);

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalPages()).isZero();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getCurrentPage()).isZero();
            then(noticeRepository).should().findAll(pageable);
        }
    }

    @Nested
    class getNoticeByUUID_테스트 {

        @Test
        void 공지사항과_사진_URL을_정상_반환한다() {
            NoticePhoto secondNoticePhoto = NoticePhoto.builder()
                    .notice(notice)
                    .noticePhotoName(SECOND_PHOTO_NAME)
                    .noticePhotoS3Key(SECOND_S3_KEY)
                    .order(2)
                    .build();
            NoticePhoto firstNoticePhoto = NoticePhoto.builder()
                    .notice(notice)
                    .noticePhotoName(FIRST_PHOTO_NAME)
                    .noticePhotoS3Key(FIRST_S3_KEY)
                    .order(1)
                    .build();
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of(secondNoticePhoto, firstNoticePhoto));
            given(s3FileUploadService.generatePresignedGetUrl(FIRST_S3_KEY)).willReturn(FIRST_PRESIGNED_URL);
            given(s3FileUploadService.generatePresignedGetUrl(SECOND_S3_KEY)).willReturn(SECOND_PRESIGNED_URL);

            NoticeDetailResponse result = adminNoticeService.getNoticeByUUID(noticeUUID);

            assertThat(result.getNoticeUUID()).isEqualTo(noticeUUID);
            assertThat(result.getNoticeTitle()).isEqualTo(NOTICE_TITLE);
            assertThat(result.getNoticeContent()).isEqualTo(NOTICE_CONTENT);
            assertThat(result.getNoticePhotos()).containsExactly(FIRST_PRESIGNED_URL, SECOND_PRESIGNED_URL);
            assertThat(result.getNoticeCreatedAt()).isEqualTo(createdAt);
            assertThat(result.getAdminName()).isEqualTo(ADMIN_NAME);
            then(noticeRepository).should().findByNoticeUUID(noticeUUID);
            then(noticePhotoRepository).should().findByNotice(notice);
        }

        @Test
        void 공지사항이_없으면_NOTICE_NOT_EXISTS_예외가_발생한다() {
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminNoticeService.getNoticeByUUID(noticeUUID))
                    .isInstanceOf(NoticeException.class)
                    .extracting(e -> ((NoticeException) e).getExceptionType())
                    .isEqualTo(ExceptionType.NOTICE_NOT_EXISTS);

            then(noticeRepository).should().findByNoticeUUID(noticeUUID);
            then(noticePhotoRepository).shouldHaveNoInteractions();
        }
    }

    @Nested
    class createNotice_테스트 {

        @Test
        void 사진_없이_공지사항을_정상_생성한다() {
            AdminNoticeCreationRequest request = new AdminNoticeCreationRequest(NOTICE_TITLE, NOTICE_CONTENT, null);
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();
                given(noticeRepository.save(any(Notice.class))).willAnswer(invocation -> invocation.getArgument(0));

                List<String> result = adminNoticeService.createNotice(request, null);

                assertThat(result).isEmpty();
                then(noticeRepository).should().save(any(Notice.class));
                then(noticePhotoRepository).shouldHaveNoInteractions();
            }
        }

        @Test
        void 사진이_있으면_공지사항과_사진을_정상_생성한다() {
            AdminNoticeCreationRequest request = new AdminNoticeCreationRequest(NOTICE_TITLE, NOTICE_CONTENT, List.of(1, 2));
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();
                given(noticeRepository.save(any(Notice.class))).willReturn(notice);
                given(firstPhoto.isEmpty()).willReturn(false);
                given(secondPhoto.isEmpty()).willReturn(false);
                given(firstPhoto.getOriginalFilename()).willReturn(FIRST_PHOTO_NAME);
                given(secondPhoto.getOriginalFilename()).willReturn(SECOND_PHOTO_NAME);
                given(s3FileUploadService.uploadFile(firstPhoto, "noticePhoto/"))
                        .willReturn(new S3FileResponse(FIRST_PRESIGNED_URL, FIRST_S3_KEY));
                given(s3FileUploadService.uploadFile(secondPhoto, "noticePhoto/"))
                        .willReturn(new S3FileResponse(SECOND_PRESIGNED_URL, SECOND_S3_KEY));

                List<String> result = adminNoticeService.createNotice(request, List.of(firstPhoto, secondPhoto));

                assertThat(result).containsExactly(FIRST_PRESIGNED_URL, SECOND_PRESIGNED_URL);
                then(noticeRepository).should().save(any(Notice.class));
                then(s3FileUploadService).should().uploadFile(firstPhoto, "noticePhoto/");
                then(s3FileUploadService).should().uploadFile(secondPhoto, "noticePhoto/");
                then(noticePhotoRepository).should().saveAll(noticePhotoListCaptor.capture());
                assertThat(noticePhotoListCaptor.getValue()).hasSize(2);
                assertThat(noticePhotoListCaptor.getValue())
                        .extracting(NoticePhoto::getNoticePhotoName)
                        .containsExactly(FIRST_PHOTO_NAME, SECOND_PHOTO_NAME);
                assertThat(noticePhotoListCaptor.getValue())
                        .extracting(NoticePhoto::getNoticePhotoS3Key)
                        .containsExactly(FIRST_S3_KEY, SECOND_S3_KEY);
                assertThat(noticePhotoListCaptor.getValue())
                        .extracting(NoticePhoto::getOrder)
                        .containsExactly(1, 2);
            }
        }

        @Test
        void 사진_수와_순서가_다르면_PHOTO_ORDER_MISMATCH_예외가_발생한다() {
            AdminNoticeCreationRequest request = new AdminNoticeCreationRequest(NOTICE_TITLE, NOTICE_CONTENT, List.of(1));
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();
                given(noticeRepository.save(any(Notice.class))).willReturn(notice);

                assertThatThrownBy(() -> adminNoticeService.createNotice(request, List.of(firstPhoto, secondPhoto)))
                        .isInstanceOf(NoticeException.class)
                        .extracting(e -> ((NoticeException) e).getExceptionType())
                        .isEqualTo(ExceptionType.PHOTO_ORDER_MISMATCH);
            }
        }

        @Test
        void 사진이_5장을_초과하면_UP_TO_5_PHOTOS_CAN_BE_UPLOADED_예외가_발생한다() {
            List<Integer> photoOrders = List.of(1, 2, 3, 4, 5, 6);
            List<MultipartFile> noticePhotos = List.of(firstPhoto, secondPhoto, firstPhoto, secondPhoto, firstPhoto, secondPhoto);
            AdminNoticeCreationRequest request = new AdminNoticeCreationRequest(NOTICE_TITLE, NOTICE_CONTENT, photoOrders);
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();
                given(noticeRepository.save(any(Notice.class))).willReturn(notice);

                assertThatThrownBy(() -> adminNoticeService.createNotice(request, noticePhotos))
                        .isInstanceOf(NoticeException.class)
                        .extracting(e -> ((NoticeException) e).getExceptionType())
                        .isEqualTo(ExceptionType.UP_TO_5_PHOTOS_CAN_BE_UPLOADED);
            }
        }
    }

    @Nested
    class updateNotice_테스트 {

        @Test
        void 공지사항이_없으면_NOTICE_NOT_EXISTS_예외가_발생한다() {
            AdminNoticeUpdateRequest request = new AdminNoticeUpdateRequest(UPDATED_NOTICE_TITLE, UPDATED_NOTICE_CONTENT, null);
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminNoticeService.updateNotice(noticeUUID, request, null))
                    .isInstanceOf(NoticeException.class)
                    .extracting(e -> ((NoticeException) e).getExceptionType())
                    .isEqualTo(ExceptionType.NOTICE_NOT_EXISTS);

            then(noticeRepository).should().findByNoticeUUID(noticeUUID);
        }

        @Test
        void 기존_사진이_없으면_공지사항을_정상_수정한다() {
            AdminNoticeUpdateRequest request = new AdminNoticeUpdateRequest(UPDATED_NOTICE_TITLE, UPDATED_NOTICE_CONTENT, null);
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of());

            List<String> result = adminNoticeService.updateNotice(noticeUUID, request, null);

            assertThat(result).isEmpty();
            assertThat(notice.getNoticeTitle()).isEqualTo(UPDATED_NOTICE_TITLE);
            assertThat(notice.getNoticeContent()).isEqualTo(UPDATED_NOTICE_CONTENT);
            then(noticePhotoRepository).should().findByNotice(notice);
            then(noticePhotoRepository).shouldHaveNoMoreInteractions();
            then(s3FileUploadService).shouldHaveNoInteractions();
        }

        @Test
        void 기존_사진이_있으면_커밋_후_S3에서_삭제한다() {
            NoticePhoto existingPhoto = NoticePhoto.builder()
                    .notice(notice)
                    .noticePhotoName(FIRST_PHOTO_NAME)
                    .noticePhotoS3Key(OLD_S3_KEY)
                    .order(1)
                    .build();
            AdminNoticeUpdateRequest request = new AdminNoticeUpdateRequest(UPDATED_NOTICE_TITLE, UPDATED_NOTICE_CONTENT, null);
            ArgumentCaptor<TransactionSynchronization> synchronizationCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of(existingPhoto));

            try (MockedStatic<TransactionSynchronizationManager> mocked = mockStatic(TransactionSynchronizationManager.class)) {
                List<String> result = adminNoticeService.updateNotice(noticeUUID, request, null);

                assertThat(result).isEmpty();
                mocked.verify(() -> TransactionSynchronizationManager.registerSynchronization(synchronizationCaptor.capture()));
                synchronizationCaptor.getValue().afterCommit();
                then(noticePhotoRepository).should().deleteAllInBatch(List.of(existingPhoto));
                then(noticePhotoRepository).shouldHaveNoMoreInteractions();
                then(s3FileUploadService).should().deleteFiles(List.of(OLD_S3_KEY));
            }
        }

        @Test
        void 새_사진이_있으면_기존_사진을_삭제하고_새_사진을_업로드한다() {
            NoticePhoto existingPhoto = NoticePhoto.builder()
                    .notice(notice)
                    .noticePhotoName("old.jpg")
                    .noticePhotoS3Key(OLD_S3_KEY)
                    .order(1)
                    .build();
            AdminNoticeUpdateRequest request = new AdminNoticeUpdateRequest(UPDATED_NOTICE_TITLE, UPDATED_NOTICE_CONTENT, List.of(2, 1));
            ArgumentCaptor<TransactionSynchronization> synchronizationCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of(existingPhoto));
            given(firstPhoto.isEmpty()).willReturn(false);
            given(secondPhoto.isEmpty()).willReturn(false);
            given(firstPhoto.getOriginalFilename()).willReturn(FIRST_PHOTO_NAME);
            given(secondPhoto.getOriginalFilename()).willReturn(SECOND_PHOTO_NAME);
            given(s3FileUploadService.uploadFile(firstPhoto, "noticePhoto/"))
                    .willReturn(new S3FileResponse(FIRST_PRESIGNED_URL, FIRST_S3_KEY));
            given(s3FileUploadService.uploadFile(secondPhoto, "noticePhoto/"))
                    .willReturn(new S3FileResponse(SECOND_PRESIGNED_URL, SECOND_S3_KEY));

            try (MockedStatic<TransactionSynchronizationManager> mocked = mockStatic(TransactionSynchronizationManager.class)) {
                List<String> result = adminNoticeService.updateNotice(noticeUUID, request, List.of(firstPhoto, secondPhoto));

                assertThat(result).containsExactly(FIRST_PRESIGNED_URL, SECOND_PRESIGNED_URL);
                mocked.verify(() -> TransactionSynchronizationManager.registerSynchronization(synchronizationCaptor.capture()));
                synchronizationCaptor.getValue().afterCommit();
                then(noticePhotoRepository).should().deleteAllInBatch(List.of(existingPhoto));
                then(s3FileUploadService).should().deleteFiles(List.of(OLD_S3_KEY));
                then(s3FileUploadService).should().uploadFile(firstPhoto, "noticePhoto/");
                then(s3FileUploadService).should().uploadFile(secondPhoto, "noticePhoto/");
                then(noticePhotoRepository).should().saveAll(noticePhotoListCaptor.capture());
                assertThat(noticePhotoListCaptor.getValue())
                        .extracting(NoticePhoto::getOrder)
                        .containsExactly(2, 1);
            }
        }
    }

    @Nested
    class deleteNotice_테스트 {

        @Test
        void 공지사항이_없으면_NOTICE_NOT_EXISTS_예외가_발생한다() {
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminNoticeService.deleteNotice(noticeUUID))
                    .isInstanceOf(NoticeException.class)
                    .extracting(e -> ((NoticeException) e).getExceptionType())
                    .isEqualTo(ExceptionType.NOTICE_NOT_EXISTS);

            then(noticeRepository).should().findByNoticeUUID(noticeUUID);
        }

        @Test
        void 사진이_없으면_공지사항만_삭제한다() {
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of());

            adminNoticeService.deleteNotice(noticeUUID);

            then(noticePhotoRepository).should().findByNotice(notice);
            then(noticeRepository).should().delete(notice);
            then(s3FileUploadService).shouldHaveNoInteractions();
        }

        @Test
        void 사진이_있으면_커밋_후_S3에서_삭제하고_공지사항을_삭제한다() {
            NoticePhoto existingPhoto = NoticePhoto.builder()
                    .notice(notice)
                    .noticePhotoName(FIRST_PHOTO_NAME)
                    .noticePhotoS3Key(OLD_S3_KEY)
                    .order(1)
                    .build();
            ArgumentCaptor<TransactionSynchronization> synchronizationCaptor = ArgumentCaptor.forClass(TransactionSynchronization.class);
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of(existingPhoto));

            try (MockedStatic<TransactionSynchronizationManager> mocked = mockStatic(TransactionSynchronizationManager.class)) {
                adminNoticeService.deleteNotice(noticeUUID);

                mocked.verify(() -> TransactionSynchronizationManager.registerSynchronization(synchronizationCaptor.capture()));
                synchronizationCaptor.getValue().afterCommit();
                then(noticePhotoRepository).should().deleteAllInBatch(List.of(existingPhoto));
                then(s3FileUploadService).should().deleteFiles(List.of(OLD_S3_KEY));
                then(noticeRepository).should().delete(notice);
            }
        }
    }
}
