package com.USWCicrcleLink.server.clubApplication.api;

import com.USWCicrcleLink.server.clubApplication.service.ClubApplicationService;
import com.USWCicrcleLink.server.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/apply")
public class ClubApplicationController {
    private final ClubApplicationService clubApplicationService;

    @GetMapping("/can-apply/{clubUUID}")
    public ResponseEntity<ApiResponse<Boolean>> canApply(@PathVariable("clubUUID") UUID clubUUID) {
        clubApplicationService.checkIfCanApply(clubUUID);
        return ResponseEntity.ok(new ApiResponse<>("지원 가능"));
    }

    @GetMapping("/{clubUUID}")
    public ResponseEntity<ApiResponse<String>> getGoogleFormUrl(@PathVariable("clubUUID") UUID clubUUID) {
        String googleFormUrl = clubApplicationService.getGoogleFormUrlByClubUUID(clubUUID);
        return ResponseEntity.ok(new ApiResponse<>("구글 폼 URL 조회 성공", googleFormUrl));
    }

    @PostMapping("/{clubUUID}")
    public ResponseEntity<ApiResponse<Void>> submitClubApplication(@PathVariable("clubUUID") UUID clubUUID) {
        clubApplicationService.submitClubApplication(clubUUID);
        return ResponseEntity.ok(new ApiResponse<>("지원서 제출 성공"));
    }
}
