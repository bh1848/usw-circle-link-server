package com.USWCicrcleLink.server.user.api;

import com.USWCicrcleLink.server.global.response.ApiResponse;
import com.USWCicrcleLink.server.user.dto.ClubFloorPhotoResponse;
import com.USWCicrcleLink.server.user.dto.MyClubApplicationResponse;
import com.USWCicrcleLink.server.user.dto.MyClubResponse;
import com.USWCicrcleLink.server.user.service.MypageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/mypages")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    @GetMapping("/my-clubs")
    public ApiResponse<List<MyClubResponse>> getMyClubById() {
        List<MyClubResponse> myclubs = mypageService.getMyClubById();
        return new ApiResponse<>("소속된 동아리 목록 조회 성공", myclubs);
    }

    @GetMapping("/club-applications")
    public ApiResponse<List<MyClubApplicationResponse>> getClubApplications() {
        List<MyClubApplicationResponse> clubApplications = mypageService.getClubApplications();
        return new ApiResponse<>("지원한 동아리 목록 조회 성공", clubApplications);
    }

    @GetMapping("/clubs/{floor}/photo")
    public ApiResponse<ClubFloorPhotoResponse> getClubFloorPhoto(@PathVariable("floor") String floor) {
        ClubFloorPhotoResponse clubFloorPhotoResponse = mypageService.getClubFloorPhoto(floor);
        return new ApiResponse<>("동아리방 층별 사진 조회 성공", clubFloorPhotoResponse);
    }
}
