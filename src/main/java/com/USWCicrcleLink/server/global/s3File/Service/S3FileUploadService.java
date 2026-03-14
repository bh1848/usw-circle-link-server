package com.USWCicrcleLink.server.global.s3File.Service;

import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.FileException;
import com.USWCicrcleLink.server.global.s3File.dto.S3FileResponse;
import com.USWCicrcleLink.server.global.validation.validator.FileSignatureValidator;
import com.amazonaws.HttpMethod;
import com.amazonaws.SdkClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
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

    private final AmazonS3 amazonS3;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("#{'${file.allowed-extensions}'.split(',')}")
    private List<String> allowedExtensions;

    private final int URL_EXPIRED_TIME = 1000 * 60 * 60;// 1시간

    // 이미지 파일 업로드
    public S3FileResponse uploadFile(MultipartFile image, String S3_PHOTO_DIR) {
        // 파일 확장자 체크
        String fileExtension = validateImageFileExtension(image);

        // 랜덤 파일명 생성 (파일명 중복 방지)
        String s3FileName = S3_PHOTO_DIR + UUID.randomUUID() + "." + fileExtension;

        log.debug("파일 업로드 준비: {}", s3FileName);

        // 사전 서명된 URL 생성 (PUT 메서드로 업로드용 URL 생성)
        String presignedUrl = generatePresignedPutUrl(s3FileName);

        log.debug("사전 서명된 URL 생성 완료: {}", presignedUrl);

        return new S3FileResponse(presignedUrl, s3FileName);
    }

    // 파일 확장 및 시그니처 확인
    private String validateImageFileExtension(MultipartFile image) {
        // 파일명 확인
        if (image == null || image.getOriginalFilename() == null) {
            throw new FileException(ExceptionType.INVALID_FILE_NAME);
        }

        String filename = image.getOriginalFilename();
        int lastDotIndex = filename.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new FileException(ExceptionType.MISSING_FILE_EXTENSION);
        }

        // 파일 확장자 추출
        String fileExtension = filename.substring(lastDotIndex + 1).toLowerCase();

        // 확장자가 허용된 파일 형식인지 확인
        if (!allowedExtensions.contains(fileExtension)) {
            throw new FileException(ExceptionType.UNSUPPORTED_FILE_EXTENSION);
        }

        // 파일 시그니처를 통해 실제 파일 형식이 올바른지 확인
        try {
            if (!FileSignatureValidator.isValidFileType(image.getInputStream(), fileExtension)) {
                throw new FileException(ExceptionType.UNSUPPORTED_FILE_EXTENSION);
            }
        } catch (IOException e) {
            throw new FileException(ExceptionType.FILE_VALIDATION_FAILED);
        }

        return fileExtension;
    }

    // 파일 업로드 URL 생성 (PUT 메서드)
    private String generatePresignedPutUrl(String fileName) {
        URL url = generatePresignedUrl(fileName, HttpMethod.PUT);
        return url != null ? url.toString() : "";
    }

    // 파일 조회 URL 생성 (GET 메서드)
    public String generatePresignedGetUrl(String fileName) {
        URL url = generatePresignedUrl(fileName, HttpMethod.GET);
        return url != null ? url.toString() : "";
    }

    // PresignedUrl 생성
    private URL generatePresignedUrl(String fileName, HttpMethod httpMethod) {
        if (fileName == null || fileName.isEmpty()) {
            log.debug("파일 이름이 비어 있습니다. presignedUrl을 생성하지 않습니다.");
            return null;
        }
        try {
            Date expiration = new Date();
            long expTimeMillis = expiration.getTime();
            expTimeMillis += URL_EXPIRED_TIME;
            expiration.setTime(expTimeMillis);

            // 사전 서명된 URL 생성
            return amazonS3.generatePresignedUrl(bucket, fileName, expiration, httpMethod);
        } catch (AmazonS3Exception e) {
            log.error("S3 사전 서명된 URL 생성 오류: " + e.getMessage());
            throw new FileException(ExceptionType.FILE_UPLOAD_FAILED);
        } catch (SdkClientException e) {
            log.error("AWS SDK 클라이언트 오류: " + e.getMessage());
            throw new FileException(ExceptionType.FILE_UPLOAD_FAILED);
        }
    }

    // 단일 삭제
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            log.warn("잘못된 S3 파일 삭제 시도: 파일 이름이 유효하지 않음 - 삭제 건너뜀");
            return;
        }

        try {
            amazonS3.deleteObject(new DeleteObjectRequest(bucket, fileName));
            log.debug("S3 파일 삭제 완료: {}", fileName);
        } catch (AmazonS3Exception e) {
            log.error("S3 파일 삭제 오류: " + e.getMessage());
            throw new FileException(ExceptionType.FILE_DELETE_FAILED);
        }
    }

    // batch 삭제
    public void deleteFiles(List<String> fileNames) {
        if (fileNames == null || fileNames.isEmpty()) {
            log.warn("잘못된 S3 파일 삭제 시도: 파일 목록이 비어 있음 - 삭제 건너뜀");
            return;
        }

        try {
            DeleteObjectsRequest deleteRequest = new DeleteObjectsRequest(bucket)
                    .withKeys(fileNames.toArray(new String[0]));

            DeleteObjectsResult result = amazonS3.deleteObjects(deleteRequest);
            log.info("S3 파일 일괄 삭제 완료: {}개 파일 삭제됨", result.getDeletedObjects().size());

        } catch (MultiObjectDeleteException e) {
            log.error("S3 파일 일부 삭제 실패: {}", e.getMessage());
            throw new FileException(ExceptionType.FILE_DELETE_FAILED);
        } catch (AmazonS3Exception e) {
            log.error("S3 파일 삭제 오류: {}", e.getMessage());
            throw new FileException(ExceptionType.FILE_DELETE_FAILED);
        }
    }
}