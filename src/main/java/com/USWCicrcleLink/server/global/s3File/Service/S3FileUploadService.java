package com.USWCicrcleLink.server.global.s3File.Service;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.FileException;
import com.USWCicrcleLink.server.global.s3File.dto.S3FileResponse;
import com.USWCicrcleLink.server.global.validation.validator.FileSignatureValidator;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.DeleteObjectsResult;
import com.amazonaws.services.s3.model.MultiObjectDeleteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class S3FileUploadService {
    private static final long PRESIGNED_URL_EXPIRATION_MILLIS = 60 * 60 * 1000L;
    private static final String EMPTY_URL = "";

    private final AmazonS3 amazonS3;
    private final FileSignatureValidator fileSignatureValidator;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("#{'${file.allowed-extensions}'.split(',')}")
    private List<String> allowedExtensions;

    public S3FileResponse uploadFile(MultipartFile image, String s3PhotoDir) {
        String fileExtension = validateImageFileExtension(image);
        String s3FileName = s3PhotoDir + UUID.randomUUID() + "." + fileExtension;

        log.debug("파일 업로드 준비: {}", s3FileName);

        String presignedUrl = generatePresignedPutUrl(s3FileName);
        log.debug("사전 서명된 URL 생성 완료: {}", presignedUrl);

        return new S3FileResponse(presignedUrl, s3FileName);
    }

    private String validateImageFileExtension(MultipartFile image) {
        if (image == null || image.getOriginalFilename() == null) {
            throw new FileException(ExceptionType.INVALID_FILE_NAME);
        }

        String filename = image.getOriginalFilename();
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            throw new FileException(ExceptionType.MISSING_FILE_EXTENSION);
        }

        String fileExtension = filename.substring(lastDotIndex + 1).toLowerCase();
        if (!allowedExtensions.contains(fileExtension)) {
            throw new FileException(ExceptionType.UNSUPPORTED_FILE_EXTENSION);
        }

        try {
            if (!fileSignatureValidator.isValidFileType(image.getInputStream(), fileExtension)) {
                throw new FileException(ExceptionType.UNSUPPORTED_FILE_EXTENSION);
            }
        } catch (IOException exception) {
            throw new FileException(ExceptionType.FILE_VALIDATION_FAILED);
        }

        return fileExtension;
    }

    private String generatePresignedPutUrl(String fileName) {
        URL url = generatePresignedUrl(fileName, HttpMethod.PUT);
        return url != null ? url.toString() : EMPTY_URL;
    }

    public String generatePresignedGetUrl(String fileName) {
        URL url = generatePresignedUrl(fileName, HttpMethod.GET);
        return url != null ? url.toString() : EMPTY_URL;
    }

    private URL generatePresignedUrl(String fileName, HttpMethod httpMethod) {
        if (fileName == null || fileName.isEmpty()) {
            log.debug("파일 이름이 비어 있어 presigned URL 생성을 건너뜁니다.");
            return null;
        }

        try {
            Date expiration = new Date(System.currentTimeMillis() + PRESIGNED_URL_EXPIRATION_MILLIS);
            return amazonS3.generatePresignedUrl(bucket, fileName, expiration, httpMethod);
        } catch (AmazonS3Exception exception) {
            log.error("S3 presigned URL 생성 오류: {}", exception.getMessage());
            throw new FileException(ExceptionType.FILE_UPLOAD_FAILED);
        } catch (SdkClientException exception) {
            log.error("AWS SDK 클라이언트 오류: {}", exception.getMessage());
            throw new FileException(ExceptionType.FILE_UPLOAD_FAILED);
        }
    }

    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            log.warn("잘못된 S3 파일 삭제 시도: 파일 이름이 비어 있어 삭제를 건너뜁니다.");
            return;
        }

        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
            log.debug("S3 파일 삭제 완료: {}", fileName);
        } catch (AmazonS3Exception exception) {
            log.error("S3 파일 삭제 오류: {}", exception.getMessage());
            throw new FileException(ExceptionType.FILE_DELETE_FAILED);
        }
    }

    public void deleteFiles(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            log.warn("잘못된 S3 파일 삭제 시도: 파일 목록이 비어 있어 삭제를 건너뜁니다.");
            return;
        }

        try {
            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucket)
                    .withKeys(fileNames.toArray(new String[0]));

            DeleteObjectsResult result = amazonS3.deleteObjects(deleteRequest);
            log.info("S3 파일 일괄 삭제 완료: {}개 파일 삭제됨", result.getDeletedObjects().size());
        } catch (MultiObjectDeleteException exception) {
            log.error("S3 파일 일부 삭제 실패: {}", exception.getMessage());
            throw new FileException(ExceptionType.FILE_DELETE_FAILED);
        } catch (AmazonS3Exception exception) {
            log.error("S3 파일 삭제 오류: {}", exception.getMessage());
            throw new FileException(ExceptionType.FILE_DELETE_FAILED);
        }
    }
}
