package com.USWCicrcleLink.server.admin.admin.api;

import com.USWCicrcleLink.server.admin.admin.dto.AdminClubCategoryCreationRequest;
import com.USWCicrcleLink.server.admin.admin.service.AdminClubCategoryService;
import com.USWCicrcleLink.server.club.club.dto.ClubCategoryResponse;
import com.USWCicrcleLink.server.global.response.ApiResponse;
import com.USWCicrcleLink.server.global.validation.support.ValidationSequence;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/clubs/category")
@RequiredArgsConstructor
public class AdminClubCategoryController {

    private final AdminClubCategoryService adminClubCategoryService;

    // 동아리 카테고리 설정 - 카테고리 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClubCategoryResponse>>> getAllClubCategories() {
        List<ClubCategoryResponse> clubCategories = adminClubCategoryService.getAllClubCategories();
        return ResponseEntity.ok(new ApiResponse<>("카테고리 리스트 조회 성공", clubCategories));
    }

    // 동아리 카테고리 설정 - 카테고리 추가
    @PostMapping
    public ResponseEntity<ApiResponse<ClubCategoryResponse>> addClubCategory(@RequestBody @Validated(ValidationSequence.class) AdminClubCategoryCreationRequest request) {
        ClubCategoryResponse addedClubCategory = adminClubCategoryService.addClubCategory(request);
        return ResponseEntity.ok(new ApiResponse<>("카테고리 추가 성공", addedClubCategory));
    }

    // 동아리 카테고리 설정 - 카테고리 삭제
    @DeleteMapping("/{clubCategoryUUID}")
    public ResponseEntity<ApiResponse<ClubCategoryResponse>> deleteClubCategory(@PathVariable("clubCategoryUUID") UUID clubCategoryUUID) {
        ClubCategoryResponse deletedCategory = adminClubCategoryService.deleteClubCategory(clubCategoryUUID);
        return ResponseEntity.ok(new ApiResponse<>("카테고리 삭제 성공", deletedCategory));
    }
}
