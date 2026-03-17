package com.USWCicrcleLink.server.global.s3File.Service;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.FileException;
import com.USWCicrcleLink.server.global.s3File.dto.S3FileResponse;
import com.USWCicrcleLink.server.global.validation.validator.FileSignatureValidator;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class S3FileUploadServiceTest {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png");
    private static final String BUCKET = "test-bucket";

    @Mock
    private AmazonS3 amazonS3;

    @Mock
    private FileSignatureValidator fileSignatureValidator;

    @InjectMocks
    private S3FileUploadService s3FileUploadService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(s3FileUploadService, "allowedExtensions", ALLOWED_EXTENSIONS);
        ReflectionTestUtils.setField(s3FileUploadService, "bucket", BUCKET);
    }

    @Nested
    class uploadFile_테스트 {

        @Test
        void jpg_파일이면_정상_업로드한다() throws Exception {
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "club.jpg",
                    "image/jpeg",
                    new byte[]{1, 2, 3}
            );
            URL presignedUrl = createUrl("https://s3.test/upload.jpg");
            given(fileSignatureValidator.isValidFileType(any(InputStream.class), eq("jpg"))).willReturn(true);
            given(amazonS3.generatePresignedUrl(eq(BUCKET), anyString(), any(Date.class), eq(HttpMethod.PUT)))
                    .willReturn(presignedUrl);

            S3FileResponse response = s3FileUploadService.uploadFile(image, "club/");
            String s3Key = response.getS3FileName();
            String uuidPart = s3Key.replace("club/", "").replace(".jpg", "");

            assertThat(response.getPresignedUrl()).isEqualTo(presignedUrl.toString());
            assertThat(s3Key).startsWith("club/").endsWith(".jpg");
            assertThatCode(() -> UUID.fromString(uuidPart)).doesNotThrowAnyException();
            then(fileSignatureValidator).should().isValidFileType(any(InputStream.class), eq("jpg"));
            then(amazonS3).should().generatePresignedUrl(eq(BUCKET), anyString(), any(Date.class), eq(HttpMethod.PUT));
        }

        @Test
        void 파일명이_null이면_INVALID_FILE_NAME_예외가_발생한다() {
            MultipartFile image = mock(MultipartFile.class);
            given(image.getOriginalFilename()).willReturn(null);

            assertThatThrownBy(() -> s3FileUploadService.uploadFile(image, "club/"))
                    .isInstanceOf(FileException.class)
                    .extracting(exception -> ((FileException) exception).getExceptionType())
                    .isEqualTo(ExceptionType.INVALID_FILE_NAME);
        }

        @Test
        void 확장자가_없으면_MISSING_FILE_EXTENSION_예외가_발생한다() {
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "club",
                    "image/jpeg",
                    new byte[]{1, 2, 3}
            );

            assertThatThrownBy(() -> s3FileUploadService.uploadFile(image, "club/"))
                    .isInstanceOf(FileException.class)
                    .extracting(exception -> ((FileException) exception).getExceptionType())
                    .isEqualTo(ExceptionType.MISSING_FILE_EXTENSION);
        }

        @Test
        void 허용되지_않는_확장자면_UNSUPPORTED_FILE_EXTENSION_예외가_발생한다() {
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "club.gif",
                    "image/gif",
                    new byte[]{1, 2, 3}
            );

            assertThatThrownBy(() -> s3FileUploadService.uploadFile(image, "club/"))
                    .isInstanceOf(FileException.class)
                    .extracting(exception -> ((FileException) exception).getExceptionType())
                    .isEqualTo(ExceptionType.UNSUPPORTED_FILE_EXTENSION);
        }

        @Test
        void 매직_바이트가_불일치하면_UNSUPPORTED_FILE_EXTENSION_예외가_발생한다() throws Exception {
            MockMultipartFile image = new MockMultipartFile(
                    "image",
                    "club.jpg",
                    "image/jpeg",
                    new byte[]{1, 2, 3}
            );
            given(fileSignatureValidator.isValidFileType(any(InputStream.class), eq("jpg"))).willReturn(false);

            assertThatThrownBy(() -> s3FileUploadService.uploadFile(image, "club/"))
                    .isInstanceOf(FileException.class)
                    .extracting(exception -> ((FileException) exception).getExceptionType())
                    .isEqualTo(ExceptionType.UNSUPPORTED_FILE_EXTENSION);
        }

        @Test
        void InputStream_읽기_중_IOException이_발생하면_FILE_VALIDATION_FAILED_예외가_발생한다() throws Exception {
            MultipartFile image = mock(MultipartFile.class);
            given(image.getOriginalFilename()).willReturn("club.jpg");
            given(image.getInputStream()).willThrow(new IOException("read fail"));

            assertThatThrownBy(() -> s3FileUploadService.uploadFile(image, "club/"))
                    .isInstanceOf(FileException.class)
                    .extracting(exception -> ((FileException) exception).getExceptionType())
                    .isEqualTo(ExceptionType.FILE_VALIDATION_FAILED);
            then(fileSignatureValidator).shouldHaveNoInteractions();
        }
    }

    @Nested
    class generatePresignedGetUrl_테스트 {

        @Test
        void 정상_파일명이면_Presigned_URL을_반환한다() throws Exception {
            URL presignedUrl = createUrl("https://s3.test/file.jpg");
            given(amazonS3.generatePresignedUrl(eq(BUCKET), eq("file.jpg"), any(Date.class), eq(HttpMethod.GET)))
                    .willReturn(presignedUrl);

            String result = s3FileUploadService.generatePresignedGetUrl("file.jpg");

            assertThat(result).isEqualTo(presignedUrl.toString());
            then(amazonS3).should().generatePresignedUrl(eq(BUCKET), eq("file.jpg"), any(Date.class), eq(HttpMethod.GET));
        }

        @Test
        void 파일명이_null이면_빈_문자열을_반환한다() {
            String result = s3FileUploadService.generatePresignedGetUrl(null);

            assertThat(result).isEmpty();
            then(amazonS3).shouldHaveNoInteractions();
        }

        @Test
        void AmazonS3Exception이_발생하면_FILE_UPLOAD_FAILED_예외가_발생한다() {
            AmazonS3Exception exception = new AmazonS3Exception("s3 error");
            given(amazonS3.generatePresignedUrl(eq(BUCKET), eq("file.jpg"), any(Date.class), eq(HttpMethod.GET)))
                    .willThrow(exception);

            assertThatThrownBy(() -> s3FileUploadService.generatePresignedGetUrl("file.jpg"))
                    .isInstanceOf(FileException.class)
                    .extracting(thrown -> ((FileException) thrown).getExceptionType())
                    .isEqualTo(ExceptionType.FILE_UPLOAD_FAILED);
        }
    }

    @Nested
    class deleteFile_테스트 {

        @Test
        void 정상_파일명이면_S3에서_삭제한다() {
            s3FileUploadService.deleteFile("file.jpg");

            ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
            then(amazonS3).should().deleteObject(captor.capture());
            assertThat(captor.getValue().getBucketName()).isEqualTo(BUCKET);
            assertThat(captor.getValue().getKey()).isEqualTo("file.jpg");
        }

        @Test
        void 파일명이_null이면_삭제를_건너뛴다() {
            s3FileUploadService.deleteFile(null);

            then(amazonS3).shouldHaveNoInteractions();
        }

        @Test
        void AmazonS3Exception이_발생하면_FILE_DELETE_FAILED_예외가_발생한다() {
            AmazonS3Exception exception = new AmazonS3Exception("delete error");
            willThrow(exception).given(amazonS3).deleteObject(any(DeleteObjectRequest.class));

            assertThatThrownBy(() -> s3FileUploadService.deleteFile("file.jpg"))
                    .isInstanceOf(FileException.class)
                    .extracting(thrown -> ((FileException) thrown).getExceptionType())
                    .isEqualTo(ExceptionType.FILE_DELETE_FAILED);
        }
    }

    @Nested
    class deleteFiles_테스트 {

        @Test
        void 정상_파일_목록이면_S3에서_일괄_삭제한다() {
            List<String> fileNames = List.of("a.jpg", "b.jpg");
            DeleteObjectsResult result = mock(DeleteObjectsResult.class);
            given(result.getDeletedObjects()).willReturn(List.of());
            given(amazonS3.deleteObjects(any(DeleteObjectsRequest.class))).willReturn(result);

            s3FileUploadService.deleteFiles(fileNames);

            ArgumentCaptor<DeleteObjectsRequest> captor = ArgumentCaptor.forClass(DeleteObjectsRequest.class);
            then(amazonS3).should().deleteObjects(captor.capture());
            assertThat(captor.getValue().getBucketName()).isEqualTo(BUCKET);
            assertThat(captor.getValue().getKeys()).extracting(DeleteObjectsRequest.KeyVersion::getKey)
                    .containsExactly("a.jpg", "b.jpg");
        }

        @Test
        void 빈_목록이면_삭제를_건너뛴다() {
            s3FileUploadService.deleteFiles(List.of());

            then(amazonS3).shouldHaveNoInteractions();
        }

        @Test
        void null_목록이면_삭제를_건너뛴다() {
            s3FileUploadService.deleteFiles(null);

            then(amazonS3).shouldHaveNoInteractions();
        }

        @Test
        void MultiObjectDeleteException이_발생하면_FILE_DELETE_FAILED_예외가_발생한다() {
            MultiObjectDeleteException exception = mock(MultiObjectDeleteException.class);
            given(amazonS3.deleteObjects(any(DeleteObjectsRequest.class))).willThrow(exception);

            assertThatThrownBy(() -> s3FileUploadService.deleteFiles(List.of("a.jpg", "b.jpg")))
                    .isInstanceOf(FileException.class)
                    .extracting(thrown -> ((FileException) thrown).getExceptionType())
                    .isEqualTo(ExceptionType.FILE_DELETE_FAILED);
        }
    }

    private URL createUrl(String value) throws MalformedURLException {
        return new URL(value);
    }
}
