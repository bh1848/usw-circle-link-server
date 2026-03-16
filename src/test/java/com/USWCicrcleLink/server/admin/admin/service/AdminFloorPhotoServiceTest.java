package com.USWCicrcleLink.server.admin.admin.service;

import com.USWCicrcleLink.server.admin.admin.dto.AdminFloorPhotoCreationResponse;
import com.USWCicrcleLink.server.club.domain.FloorPhoto;
import com.USWCicrcleLink.server.club.domain.FloorPhotoEnum;
import com.USWCicrcleLink.server.club.repository.FloorPhotoRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.PhotoException;
import com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService;
import com.USWCicrcleLink.server.global.s3File.dto.S3FileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AdminFloorPhotoServiceTest {

    private static final String PHOTO_NAME = "floor-photo.jpg";
    private static final String EXISTING_S3_KEY = "floorPhoto/old-photo.jpg";
    private static final String NEW_S3_KEY = "floorPhoto/new-photo.jpg";
    private static final String PRESIGNED_URL = "https://presigned.example.com/floor-photo";

    @Mock private FloorPhotoRepository floorPhotoRepository;
    @Mock private S3FileUploadService s3FileUploadService;
    @Mock private MultipartFile photo;

    @InjectMocks
    private AdminFloorPhotoService adminFloorPhotoService;

    private FloorPhotoEnum floor;
    private FloorPhoto existingFloorPhoto;

    @BeforeEach
    void setUp() {
        floor = FloorPhotoEnum.F1;
        existingFloorPhoto = FloorPhoto.builder()
                .floorPhotoId(1L)
                .floorPhotoName(PHOTO_NAME)
                .floorPhotoS3Key(EXISTING_S3_KEY)
                .floor(floor)
                .build();
    }

    @Nested
    class uploadPhoto_테스트 {

        @Test
        void 빈_사진이면_PHOTO_FILE_IS_EMPTY_예외가_발생한다() {
            given(photo.isEmpty()).willReturn(true);

            assertThatThrownBy(() -> adminFloorPhotoService.uploadPhoto(floor, photo))
                    .isInstanceOf(PhotoException.class)
                    .extracting(e -> ((PhotoException) e).getExceptionType())
                    .isEqualTo(ExceptionType.PHOTO_FILE_IS_EMPTY);

            then(floorPhotoRepository).shouldHaveNoInteractions();
            then(s3FileUploadService).shouldHaveNoInteractions();
        }

        @Test
        void 사진이_null이면_PHOTO_FILE_IS_EMPTY_예외가_발생한다() {
            assertThatThrownBy(() -> adminFloorPhotoService.uploadPhoto(floor, null))
                    .isInstanceOf(PhotoException.class)
                    .extracting(e -> ((PhotoException) e).getExceptionType())
                    .isEqualTo(ExceptionType.PHOTO_FILE_IS_EMPTY);

            then(floorPhotoRepository).shouldHaveNoInteractions();
            then(s3FileUploadService).shouldHaveNoInteractions();
        }

        @Test
        void 기존_사진이_없으면_새_사진을_업로드하고_저장한다() {
            ArgumentCaptor<FloorPhoto> floorPhotoCaptor = ArgumentCaptor.forClass(FloorPhoto.class);
            given(photo.isEmpty()).willReturn(false);
            given(photo.getOriginalFilename()).willReturn(PHOTO_NAME);
            given(floorPhotoRepository.findByFloor(floor)).willReturn(Optional.empty());
            given(s3FileUploadService.uploadFile(photo, "floorPhoto/"))
                    .willReturn(new S3FileResponse(PRESIGNED_URL, NEW_S3_KEY));

            AdminFloorPhotoCreationResponse result = adminFloorPhotoService.uploadPhoto(floor, photo);

            assertThat(result.getFloor()).isEqualTo(floor);
            assertThat(result.getPresignedUrl()).isEqualTo(PRESIGNED_URL);
            then(floorPhotoRepository).should().findByFloor(floor);
            then(s3FileUploadService).should().uploadFile(photo, "floorPhoto/");
            then(floorPhotoRepository).should().save(floorPhotoCaptor.capture());
            FloorPhoto savedFloorPhoto = floorPhotoCaptor.getValue();
            assertThat(savedFloorPhoto.getFloor()).isEqualTo(floor);
            assertThat(savedFloorPhoto.getFloorPhotoName()).isEqualTo(PHOTO_NAME);
            assertThat(savedFloorPhoto.getFloorPhotoS3Key()).isEqualTo(NEW_S3_KEY);
        }

        @Test
        void 기존_사진이_있으면_삭제_후_새_사진으로_교체한다() {
            ArgumentCaptor<FloorPhoto> floorPhotoCaptor = ArgumentCaptor.forClass(FloorPhoto.class);
            given(photo.isEmpty()).willReturn(false);
            given(photo.getOriginalFilename()).willReturn(PHOTO_NAME);
            given(floorPhotoRepository.findByFloor(floor)).willReturn(Optional.of(existingFloorPhoto));
            given(s3FileUploadService.uploadFile(photo, "floorPhoto/"))
                    .willReturn(new S3FileResponse(PRESIGNED_URL, NEW_S3_KEY));

            AdminFloorPhotoCreationResponse result = adminFloorPhotoService.uploadPhoto(floor, photo);

            assertThat(result.getFloor()).isEqualTo(floor);
            assertThat(result.getPresignedUrl()).isEqualTo(PRESIGNED_URL);
            then(s3FileUploadService).should().deleteFile(EXISTING_S3_KEY);
            then(floorPhotoRepository).should().delete(existingFloorPhoto);
            then(s3FileUploadService).should().uploadFile(photo, "floorPhoto/");
            then(floorPhotoRepository).should().save(floorPhotoCaptor.capture());
            assertThat(floorPhotoCaptor.getValue().getFloorPhotoS3Key()).isEqualTo(NEW_S3_KEY);
        }
    }

    @Nested
    class getPhotoByFloor_테스트 {

        @Test
        void 층별_사진이_있으면_Presigned_URL을_반환한다() {
            given(floorPhotoRepository.findByFloor(floor)).willReturn(Optional.of(existingFloorPhoto));
            given(s3FileUploadService.generatePresignedGetUrl(EXISTING_S3_KEY)).willReturn(PRESIGNED_URL);

            AdminFloorPhotoCreationResponse result = adminFloorPhotoService.getPhotoByFloor(floor);

            assertThat(result.getFloor()).isEqualTo(floor);
            assertThat(result.getPresignedUrl()).isEqualTo(PRESIGNED_URL);
            then(floorPhotoRepository).should().findByFloor(floor);
            then(s3FileUploadService).should().generatePresignedGetUrl(EXISTING_S3_KEY);
        }

        @Test
        void 층별_사진이_없으면_PHOTO_NOT_FOUND_예외가_발생한다() {
            given(floorPhotoRepository.findByFloor(floor)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminFloorPhotoService.getPhotoByFloor(floor))
                    .isInstanceOf(PhotoException.class)
                    .extracting(e -> ((PhotoException) e).getExceptionType())
                    .isEqualTo(ExceptionType.PHOTO_NOT_FOUND);

            then(floorPhotoRepository).should().findByFloor(floor);
            then(s3FileUploadService).shouldHaveNoInteractions();
        }
    }

    @Nested
    class deletePhotoByFloor_테스트 {

        @Test
        void 층별_사진이_있으면_S3와_DB에서_정상_삭제한다() {
            given(floorPhotoRepository.findByFloor(floor)).willReturn(Optional.of(existingFloorPhoto));

            adminFloorPhotoService.deletePhotoByFloor(floor);

            then(floorPhotoRepository).should().findByFloor(floor);
            then(s3FileUploadService).should().deleteFile(EXISTING_S3_KEY);
            then(floorPhotoRepository).should().delete(existingFloorPhoto);
        }

        @Test
        void 층별_사진이_없으면_PHOTO_NOT_FOUND_예외가_발생한다() {
            given(floorPhotoRepository.findByFloor(floor)).willReturn(Optional.empty());

            assertThatThrownBy(() -> adminFloorPhotoService.deletePhotoByFloor(floor))
                    .isInstanceOf(PhotoException.class)
                    .extracting(e -> ((PhotoException) e).getExceptionType())
                    .isEqualTo(ExceptionType.PHOTO_NOT_FOUND);

            then(floorPhotoRepository).should().findByFloor(floor);
            then(s3FileUploadService).shouldHaveNoInteractions();
        }
    }
}
