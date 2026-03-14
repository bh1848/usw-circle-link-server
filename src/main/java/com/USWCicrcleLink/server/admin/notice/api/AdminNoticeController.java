package com.USWCicrcleLink.server.admin.notice.api;

import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticeCreationRequest;
import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticePageListResponse;
import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticeUpdateRequest;
import com.USWCicrcleLink.server.admin.notice.dto.NoticeDetailResponse;
import com.USWCicrcleLink.server.admin.notice.service.AdminNoticeService;
import com.USWCicrcleLink.server.global.response.ApiResponse;
import com.USWCicrcleLink.server.global.validation.support.ValidationSequence;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notices")
@RequiredArgsConstructor
public class AdminNoticeController {
    private final AdminNoticeService noticeService;

    // 공지사항 리스트 조회
    @GetMapping
    public ResponseEntity<ApiResponse<AdminNoticePageListResponse>> getNotices(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("noticeCreatedAt").descending());
        AdminNoticePageListResponse pagedNotices = noticeService.getNotices(pageable);
        return ResponseEntity.ok(new ApiResponse<>("공지사항 리스트 조회 성공", pagedNotices));
    }

    // 공지사항 세부내용 조회 (ADMIN, LEADER)
    @GetMapping("/{noticeUUID}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getNoticeByUUID(@PathVariable("noticeUUID") UUID noticeUUID) {
        NoticeDetailResponse notice = noticeService.getNoticeByUUID(noticeUUID);
        ApiResponse<NoticeDetailResponse> response = new ApiResponse<>("공지사항 조회 성공", notice);
        return ResponseEntity.ok(response);
    }

    // 공지사항 생성 (ADMIN)
    @PostMapping
    public ResponseEntity<ApiResponse<List<String>>> createNotice(
            @RequestPart(value = "request", required = false) @Validated(ValidationSequence.class) AdminNoticeCreationRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> noticePhotos) {
        List<String> presignedUrls = noticeService.createNotice(request, noticePhotos);
        return ResponseEntity.ok(new ApiResponse<>("공지사항 생성 성공", presignedUrls));
    }

    // 공지사항 수정 (ADMIN)
    @PutMapping("/{noticeUUID}")
    public ResponseEntity<ApiResponse<List<String>>> updateNotice(
            @PathVariable("noticeUUID") UUID noticeUUID,
            @RequestPart(value = "request", required = false) @Validated(ValidationSequence.class) AdminNoticeUpdateRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> noticePhotos) {

        List<String> presignedUrls = noticeService.updateNotice(noticeUUID, request, noticePhotos);
        ApiResponse<List<String>> response = new ApiResponse<>("공지사항 수정 성공", presignedUrls);
        return ResponseEntity.ok(response);
    }

    // 공지사항 삭제 (ADMIN)
    @DeleteMapping("/{noticeUUID}")
    public ResponseEntity<ApiResponse<UUID>> deleteNotice(@PathVariable("noticeUUID") UUID noticeUUID) {
        noticeService.deleteNotice(noticeUUID);
        ApiResponse<UUID> response = new ApiResponse<>("공지사항 삭제 성공", noticeUUID);
        return ResponseEntity.ok(response);
    }
}
