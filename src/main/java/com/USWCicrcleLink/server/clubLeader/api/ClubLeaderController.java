package com.USWCicrcleLink.server.clubLeader.api;

import com.USWCicrcleLink.server.admin.admin.service.AdminClubCategoryService;
import com.USWCicrcleLink.server.clubApplication.dto.ApplicantResultsRequest;
import com.USWCicrcleLink.server.club.dto.ClubCategoryResponse;
import com.USWCicrcleLink.server.clubLeader.dto.FcmTokenRequest;
import com.USWCicrcleLink.server.clubLeader.dto.club.*;
import com.USWCicrcleLink.server.clubLeader.dto.clubMembers.*;
import com.USWCicrcleLink.server.clubLeader.service.ClubLeaderService;
import com.USWCicrcleLink.server.clubLeader.service.FcmServiceImpl;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.ProfileException;
import com.USWCicrcleLink.server.global.response.ApiResponse;
import com.USWCicrcleLink.server.global.validation.support.ValidationSequence;
import com.USWCicrcleLink.server.profile.domain.MemberType;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/club-leader")
@Slf4j
public class ClubLeaderController {

    private final ClubLeaderService clubLeaderService;
    private final AdminClubCategoryService adminClubCategoryService;
    private final FcmServiceImpl fcmService;

    // 약관 동의 완료 업데이트
    @PatchMapping("/terms/agreement")
    public ResponseEntity<ApiResponse<String>> SetAgreedTermsTrue (){
        clubLeaderService.updateAgreedTermsTrue();
        return new ResponseEntity<>(new ApiResponse<>("약관 동의 완료"),HttpStatus.OK);
    }

    // 동아리 기본 정보 조회
    @GetMapping("/{clubUUID}/info")
    public ResponseEntity<ApiResponse> getClubInfo(@PathVariable("clubUUID")UUID clubUUID) {
        ApiResponse<ClubInfoResponse> clubInfo = clubLeaderService.getClubInfo(clubUUID);
        return new ResponseEntity<>(clubInfo, HttpStatus.OK);
    }


    // 동아리 기본 정보 변경 - 카테고리 조회
    @GetMapping("/category")
    public ResponseEntity<ApiResponse<List<ClubCategoryResponse>>> getAllCategories() {
        List<ClubCategoryResponse> categories = adminClubCategoryService.getAllClubCategories();
        return ResponseEntity.ok(new ApiResponse<>("카테고리 리스트 조회 성공", categories));
    }

    // 동아리 기본 정보 변경
    @PutMapping("/{clubUUID}/info")
    public ResponseEntity<ApiResponse> updateClubInfo(@PathVariable("clubUUID") UUID clubUUID,
                                                      @RequestPart(value = "mainPhoto", required = false) MultipartFile mainPhoto,
                                                      @RequestPart(value = "clubInfoRequest", required = false) @Validated(ValidationSequence.class) ClubInfoRequest clubInfoRequest) throws IOException {

        return new ResponseEntity<>(clubLeaderService.updateClubInfo(clubUUID, clubInfoRequest, mainPhoto), HttpStatus.OK);
    }

    // 동아리 요약 조회
    @GetMapping("/{clubUUID}/summary")
    public ResponseEntity<ApiResponse<ClubSummaryResponse>> getClubSummary(@PathVariable("clubUUID") UUID clubUUID) {
        ClubSummaryResponse clubIntroWebResponse = clubLeaderService.getClubSummary(clubUUID);
        ApiResponse<ClubSummaryResponse> response = new ApiResponse<>("동아리 요약 조회 완료", clubIntroWebResponse);
        return ResponseEntity.ok(response);
    }

    // 동아리 소개 조회
    @GetMapping("/{clubUUID}/intro")
    public ResponseEntity<ApiResponse<LeaderClubIntroResponse>> getClubIntro(@PathVariable("clubUUID") UUID clubUUID) {
        return new ResponseEntity<>(clubLeaderService.getClubIntro(clubUUID), HttpStatus.OK);
    }

    // 동아리 소개 변경
    @PutMapping("/{clubUUID}/intro")
    public ResponseEntity<ApiResponse> updateClubIntro(@PathVariable("clubUUID") UUID clubUUID,
                                                       @RequestPart(value = "clubIntroRequest", required = false) @Valid ClubIntroRequest clubIntroRequest,
                                                       @RequestPart(value = "introPhotos", required = false) List<MultipartFile> introPhotos) throws IOException {

        return new ResponseEntity<>(clubLeaderService.updateClubIntro(clubUUID, clubIntroRequest, introPhotos), HttpStatus.OK);
    }

    // 동아리 모집 상태 변경
    @PatchMapping("/{clubUUID}/recruitment")
    public ResponseEntity<ApiResponse> toggleRecruitmentStatus(@PathVariable("clubUUID") UUID clubUUID) {
        return new ResponseEntity<>(clubLeaderService.toggleRecruitmentStatus(clubUUID), HttpStatus.OK);
    }

//    @GetMapping("/v1/members")
//    public ResponseEntity<ApiResponse> getClubMembers(LeaderToken token) {
//        // 원래는 GET 요청임 토큰때문
//        return new ResponseEntity<>(clubLeaderService.findClubMembers(token), HttpStatus.OK);
//    }

    // 소속 동아리 회원 조회
    @GetMapping("/{clubUUID}/members")
    public ResponseEntity<ApiResponse> getClubMembers(
            @PathVariable("clubUUID") UUID clubUUID,
            @RequestParam(value = "sort", defaultValue = "default") String sort) {

        ApiResponse<List<ClubMembersResponse>> response = switch (sort.toLowerCase()) {
            case "regular-member" -> clubLeaderService.getClubMembersByMemberType(clubUUID, MemberType.REGULARMEMBER);
            case "non-member" -> clubLeaderService.getClubMembersByMemberType(clubUUID, MemberType.NONMEMBER);
            case "default" -> clubLeaderService.getClubMembers(clubUUID);
            default -> throw new ProfileException(ExceptionType.INVALID_MEMBER_TYPE);
        };

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // 동아리 회원 퇴출
    @DeleteMapping("/{clubUUID}/members")
    public ResponseEntity<ApiResponse> deleteClubMembers(@PathVariable("clubUUID") UUID clubUUID, @RequestBody List<ClubMembersDeleteRequest> clubMemberUUIDList) {
        return new ResponseEntity<>(clubLeaderService.deleteClubMembers(clubUUID, clubMemberUUIDList), HttpStatus.OK);
    }

    // 동아리 회원 엑셀 파일 추출
    @GetMapping("/{clubUUID}/members/export")
    public ResponseEntity<ApiResponse> exportClubMembers(@PathVariable("clubUUID") UUID clubUUID, HttpServletResponse response) {
        // 엑셀 파일 생성
        clubLeaderService.downloadExcel(clubUUID, response);
        return new ResponseEntity<>(new ApiResponse<>("동아리 회원 엑셀 파일 내보내기 완료"), HttpStatus.OK);
    }

    // fcm 토큰 갱신
    @PatchMapping("/fcmtoken")
    public ResponseEntity<ApiResponse> updateFcmToken(@RequestBody FcmTokenRequest fcmTokenRequest) {
        fcmService.refreshFcmToken(fcmTokenRequest);
        return new ResponseEntity<>(new ApiResponse<>("fcm token 갱신 완료"), HttpStatus.OK);
    }

    // 최초 지원자 조회
    @GetMapping("/{clubUUID}/applicants")
    public ResponseEntity<ApiResponse> getApplicants(@PathVariable("clubUUID") UUID clubUUID) {
        return new ResponseEntity<>(clubLeaderService.getApplicants(clubUUID), HttpStatus.OK);
    }

    // 최초 합격자 알림
    @PostMapping("/{clubUUID}/applicants/notifications")
    public ResponseEntity<ApiResponse> pushApplicantResults(@PathVariable("clubUUID") UUID clubUUID, @RequestBody @Validated(ValidationSequence.class) List<ApplicantResultsRequest> results) throws IOException {
        clubLeaderService.updateApplicantResults(clubUUID, results);
        return new ResponseEntity<>(new ApiResponse<>("지원 결과 처리 완료"), HttpStatus.OK);
    }

    // 불합격자 조회
    @GetMapping("/{clubUUID}/failed-applicants")
    public ResponseEntity<ApiResponse> getFailedApplicants(@PathVariable("clubUUID") UUID clubUUID) {
        return new ResponseEntity<>(clubLeaderService.getFailedApplicants(clubUUID), HttpStatus.OK);
    }

    // 지원자 추가 합격 알림
    @PostMapping("/{clubUUID}/failed-applicants/notifications")
    public ResponseEntity<ApiResponse> pushFailedApplicantResults(@PathVariable("clubUUID") UUID clubUUID, @RequestBody @Validated(ValidationSequence.class) List<ApplicantResultsRequest> results) throws IOException {
        clubLeaderService.updateFailedApplicantResults(clubUUID, results);
        return new ResponseEntity<>(new ApiResponse<>("추합 결과 처리 완료"), HttpStatus.OK);
    }

    // 기존 동아리 회원 엑셀 파일 업로드
    @PostMapping("/{clubUUID}/members/import")
    public ResponseEntity<ApiResponse<ClubMembersImportExcelResponse>> importClubMembers(@PathVariable("clubUUID") UUID clubUUID, @RequestPart(value = "clubMembersFile", required = true) MultipartFile clubMembersFile) throws IOException {
        return new ResponseEntity<>(clubLeaderService.uploadExcel(clubUUID, clubMembersFile), HttpStatus.OK);
    }

    // 기존 동아리 회원 엑셀 파일로 추가
    @PostMapping("/{clubUUID}/members")
    public ResponseEntity<ApiResponse> addClubMembersFromExcel(@PathVariable("clubUUID") UUID clubUUID, @RequestBody @Validated(ValidationSequence.class) ClubMembersAddFromExcelRequestList clubMembersAddFromExcelRequestList) {
        clubLeaderService.addClubMembersFromExcel(clubUUID, clubMembersAddFromExcelRequestList.getClubMembersAddFromExcelRequestList());
        return new ResponseEntity<>(new ApiResponse<>("엑셀로 추가된 기존 동아리 회원 저장 완료"), HttpStatus.OK);
    }

    // 프로필 중복 동아리 회원 추가
    @PostMapping("/{clubUUID}/members/duplicate-profiles")
    public ResponseEntity<ApiResponse> getDuplicateProfileMember(@PathVariable("clubUUID") UUID clubUUID, @RequestBody @Validated(ValidationSequence.class) DuplicateProfileMemberRequest duplicateProfileMemberRequest) {
        return new ResponseEntity<>(clubLeaderService.addDuplicateProfileMember(clubUUID, duplicateProfileMemberRequest), HttpStatus.OK);
    }

    // 비회원 프로필 업데이트
    @PatchMapping("/{clubUUID}/members/{clubMemberUUID}/non-member")
    public ResponseEntity<ApiResponse> updateNonMemberProfile(@PathVariable("clubUUID") UUID clubUUID,
                                                              @PathVariable("clubMemberUUID") UUID clubMemberUUID,
                                                              @RequestBody @Validated(ValidationSequence.class) ClubNonMemberUpdateRequest clubNonMemberUpdateRequest) {
        return new ResponseEntity<>(clubLeaderService.updateNonMemberProfile(clubUUID, clubMemberUUID, clubNonMemberUpdateRequest), HttpStatus.OK);
    }

    // 기존 동아리 회원 가입 요청 조회
    @GetMapping("/{clubUUID}/members/sign-up")
    public ResponseEntity<ApiResponse> getSignUpRequest(@PathVariable("clubUUID") UUID clubUUID) {
        return new ResponseEntity<>(clubLeaderService.getSignUpRequest(clubUUID), HttpStatus.OK);
    }

    // 기존 동아리 회원 가입 요청 삭제(거절)
    @DeleteMapping("/{clubUUID}/members/sign-up/{clubMemberAccountStatusUUID}")
    public ResponseEntity<ApiResponse> deleteSignUpRequest(@PathVariable("clubUUID") UUID clubUUID, @PathVariable("clubMemberAccountStatusUUID") UUID clubMemberAccountStatusUUID) {
        return new ResponseEntity<>(clubLeaderService.deleteSignUpRequest(clubUUID, clubMemberAccountStatusUUID), HttpStatus.OK);
    }

    // 기존 동아리 회원 가입 요청 수락
    @PostMapping("/{clubUUID}/members/sign-up")
    public ResponseEntity<ApiResponse> acceptSignUpRequest(@PathVariable("clubUUID") UUID clubUUID, @RequestBody @Validated(ValidationSequence.class) ClubMembersAcceptSignUpRequest clubMembersAcceptSignUpRequest) {
        return new ResponseEntity<>(clubLeaderService.acceptSignUpRequest(clubUUID, clubMembersAcceptSignUpRequest), HttpStatus.OK);
    }

}
