package com.USWCicrcleLink.server.admin.admin.api;

import com.USWCicrcleLink.server.admin.admin.dto.AdminFloorPhotoCreationResponse;
import com.USWCicrcleLink.server.admin.admin.service.AdminFloorPhotoService;
import com.USWCicrcleLink.server.club.club.domain.FloorPhotoEnum;
import com.USWCicrcleLink.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/floor/photo")
@RequiredArgsConstructor
public class AdminFloorPhotoController {

    private final AdminFloorPhotoService adminFloorPhotoService;

    @PutMapping("/{floor}")
    public ResponseEntity<ApiResponse<AdminFloorPhotoCreationResponse>> uploadFloorPhoto(
            @PathVariable("floor") FloorPhotoEnum floor,
            @RequestPart("photo") MultipartFile photo) {
        AdminFloorPhotoCreationResponse photoResponse = adminFloorPhotoService.uploadPhoto(floor, photo);
        return ResponseEntity.ok(new ApiResponse<>("해당 층 사진 업로드 성공", photoResponse));
    }

    @GetMapping("/{floor}")
    public ResponseEntity<ApiResponse<AdminFloorPhotoCreationResponse>> getPhotoByFloor(
            @PathVariable("floor") FloorPhotoEnum floor) {
        AdminFloorPhotoCreationResponse photoResponse = adminFloorPhotoService.getPhotoByFloor(floor);
        return ResponseEntity.ok(new ApiResponse<>("해당 층 사진 조회 성공", photoResponse));
    }

    @DeleteMapping("/{floor}")
    public ResponseEntity<ApiResponse<String>> deletePhotoByFloor(
            @PathVariable("floor") FloorPhotoEnum floor) {
        adminFloorPhotoService.deletePhotoByFloor(floor);
        return ResponseEntity.ok(new ApiResponse<>("해당 층 사진 삭제 성공", "Floor: " + floor.name()));
    }
}
