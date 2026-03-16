package com.USWCicrcleLink.server.admin.notice.service;

import com.USWCicrcleLink.server.admin.admin.domain.Admin;
import com.USWCicrcleLink.server.admin.notice.domain.Notice;
import com.USWCicrcleLink.server.admin.notice.domain.NoticePhoto;
import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticeCreationRequest;
import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticeUpdateRequest;
import com.USWCicrcleLink.server.admin.notice.repository.NoticePhotoRepository;
import com.USWCicrcleLink.server.admin.notice.repository.NoticeRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.NoticeException;
import com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService;
import com.USWCicrcleLink.server.global.security.details.CustomAdminDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminNoticeServiceTest {

    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticePhotoRepository noticePhotoRepository;
    @Mock private S3FileUploadService s3FileUploadService;

    @InjectMocks
    private AdminNoticeService adminNoticeService;

    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;
    @Mock private Notice notice;

    private Admin admin;
    private UUID noticeUUID;

    @BeforeEach
    void setUp() {
        noticeUUID = UUID.randomUUID();
        admin = mock(Admin.class);
    }

    private void mockAuthentication() {
        given(securityContext.getAuthentication()).willReturn(authentication);
        given(authentication.getPrincipal()).willReturn(new CustomAdminDetails(admin));
    }


    @Nested
    class createNotice_테스트 {

        @Test
        void 사진_없이_공지사항_정상_생성() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminNoticeCreationRequest request = new AdminNoticeCreationRequest("제목", "내용", null);
                given(noticeRepository.save(any(Notice.class))).willAnswer(inv -> inv.getArgument(0));

                List<String> result = adminNoticeService.createNotice(request, null);

                assertThat(result).isEmpty();
                verify(noticeRepository).save(any(Notice.class));
            }
        }

        @Test
        void 사진_수와_순서_불일치시_PHOTO_ORDER_MISMATCH_예외() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                AdminNoticeCreationRequest request = new AdminNoticeCreationRequest("제목", "내용", List.of(1));
                given(noticeRepository.save(any(Notice.class))).willAnswer(inv -> inv.getArgument(0));

                List<MultipartFile> photos = List.of(mock(MultipartFile.class), mock(MultipartFile.class));

                assertThatThrownBy(() -> adminNoticeService.createNotice(request, photos))
                        .isInstanceOf(NoticeException.class)
                        .extracting(e -> ((NoticeException) e).getExceptionType())
                        .isEqualTo(ExceptionType.PHOTO_ORDER_MISMATCH);
            }
        }

        @Test
        void 사진_5장_초과시_UP_TO_5_PHOTOS_예외() {
            try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
                mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
                mockAuthentication();

                List<Integer> orders = List.of(1, 2, 3, 4, 5, 6);
                AdminNoticeCreationRequest request = new AdminNoticeCreationRequest("제목", "내용", orders);
                given(noticeRepository.save(any(Notice.class))).willAnswer(inv -> inv.getArgument(0));

                List<MultipartFile> photos = List.of(
                        mock(MultipartFile.class), mock(MultipartFile.class), mock(MultipartFile.class),
                        mock(MultipartFile.class), mock(MultipartFile.class), mock(MultipartFile.class)
                );

                assertThatThrownBy(() -> adminNoticeService.createNotice(request, photos))
                        .isInstanceOf(NoticeException.class)
                        .extracting(e -> ((NoticeException) e).getExceptionType())
                        .isEqualTo(ExceptionType.UP_TO_5_PHOTOS_CAN_BE_UPLOADED);
            }
        }
    }


    @Nested
    class updateNotice_테스트 {

        @Test
        void 공지사항_존재하지_않으면_NOTICE_NOT_EXISTS_예외() {
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.empty());

            AdminNoticeUpdateRequest request = new AdminNoticeUpdateRequest("제목", "내용", null);

            assertThatThrownBy(() -> adminNoticeService.updateNotice(noticeUUID, request, null))
                    .isInstanceOf(NoticeException.class)
                    .extracting(e -> ((NoticeException) e).getExceptionType())
                    .isEqualTo(ExceptionType.NOTICE_NOT_EXISTS);
        }

        @Test
        void 기존_사진_없을때_공지사항_정상_수정() {
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of());

            AdminNoticeUpdateRequest request = new AdminNoticeUpdateRequest("새제목", "새내용", null);

            List<String> result = adminNoticeService.updateNotice(noticeUUID, request, null);

            assertThat(result).isEmpty();
            verify(noticePhotoRepository, never()).deleteAllInBatch(any());
            verify(s3FileUploadService, never()).deleteFiles(any());
        }

        @Test
        void 기존_사진_있을때_커밋_후_S3_삭제_실행() {
            ArgumentCaptor<TransactionSynchronization> syncCaptor =
                    ArgumentCaptor.forClass(TransactionSynchronization.class);

            NoticePhoto photo = mock(NoticePhoto.class);
            given(photo.getNoticePhotoS3Key()).willReturn("noticePhoto/old.jpg");
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of(photo));

            AdminNoticeUpdateRequest request = new AdminNoticeUpdateRequest("새제목", "새내용", null);

            try (MockedStatic<TransactionSynchronizationManager> txMocked =
                         mockStatic(TransactionSynchronizationManager.class)) {

                adminNoticeService.updateNotice(noticeUUID, request, null);

                txMocked.verify(() ->
                        TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
                syncCaptor.getValue().afterCommit();

                verify(s3FileUploadService).deleteFiles(List.of("noticePhoto/old.jpg"));
                verify(noticePhotoRepository).deleteAllInBatch(List.of(photo));
            }
        }
    }


    @Nested
    class deleteNotice_테스트 {

        @Test
        void 공지사항_존재하지_않으면_NOTICE_NOT_EXISTS_예외() {
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminNoticeService.deleteNotice(noticeUUID))
                    .isInstanceOf(NoticeException.class)
                    .extracting(e -> ((NoticeException) e).getExceptionType())
                    .isEqualTo(ExceptionType.NOTICE_NOT_EXISTS);
        }

        @Test
        void 사진_없을때_정상_삭제_S3_미호출() {
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of());

            adminNoticeService.deleteNotice(noticeUUID);

            verify(noticeRepository).delete(notice);
            verify(s3FileUploadService, never()).deleteFiles(any());
        }

        @Test
        void 사진_있을때_커밋_후_S3_삭제_실행() {
            ArgumentCaptor<TransactionSynchronization> syncCaptor =
                    ArgumentCaptor.forClass(TransactionSynchronization.class);

            NoticePhoto photo = mock(NoticePhoto.class);
            given(photo.getNoticePhotoS3Key()).willReturn("noticePhoto/test.jpg");
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of(photo));

            try (MockedStatic<TransactionSynchronizationManager> txMocked =
                         mockStatic(TransactionSynchronizationManager.class)) {

                adminNoticeService.deleteNotice(noticeUUID);

                txMocked.verify(() ->
                        TransactionSynchronizationManager.registerSynchronization(syncCaptor.capture()));
                syncCaptor.getValue().afterCommit();

                verify(s3FileUploadService).deleteFiles(List.of("noticePhoto/test.jpg"));
                verify(noticePhotoRepository).deleteAllInBatch(List.of(photo));
                verify(noticeRepository).delete(notice);
            }
        }

        @Test
        void 트랜잭션_커밋_전에는_S3_삭제_미실행() {
            NoticePhoto photo = mock(NoticePhoto.class);
            given(photo.getNoticePhotoS3Key()).willReturn("noticePhoto/test.jpg");
            given(noticeRepository.findByNoticeUUID(noticeUUID)).willReturn(Optional.of(notice));
            given(noticePhotoRepository.findByNotice(notice)).willReturn(List.of(photo));

            try (MockedStatic<TransactionSynchronizationManager> ignored =
                         mockStatic(TransactionSynchronizationManager.class)) {

                adminNoticeService.deleteNotice(noticeUUID);

                // afterCommit()을 호출하지 않으면 S3 삭제 미실행
                verify(s3FileUploadService, never()).deleteFiles(any());
            }
        }
    }
}
