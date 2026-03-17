package com.USWCicrcleLink.server.clubLeader.service;

import com.USWCicrcleLink.server.club.domain.*;
import com.USWCicrcleLink.server.club.repository.*;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import com.USWCicrcleLink.server.clubApplication.dto.ApplicantResultsRequest;
import com.USWCicrcleLink.server.clubApplication.dto.ApplicantsResponse;
import com.USWCicrcleLink.server.clubApplication.repository.ClubApplicationRepository;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntro;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntroPhoto;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroPhotoRepository;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroRepository;
import com.USWCicrcleLink.server.clubLeader.domain.Leader;
import com.USWCicrcleLink.server.clubLeader.dto.club.*;
import com.USWCicrcleLink.server.clubLeader.dto.clubMembers.*;
import com.USWCicrcleLink.server.clubLeader.repository.LeaderRepository;
import com.USWCicrcleLink.server.clubLeader.util.ClubMemberExcelDataDto;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.*;
import com.USWCicrcleLink.server.global.response.ApiResponse;
import com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService;
import com.USWCicrcleLink.server.global.s3File.dto.S3FileResponse;
import com.USWCicrcleLink.server.global.security.details.CustomLeaderDetails;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import com.USWCicrcleLink.server.global.validation.validator.FileSignatureValidator;
import com.USWCicrcleLink.server.profile.domain.MemberType;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import com.USWCicrcleLink.server.user.domain.ExistingMember.ClubMemberAccountStatus;
import com.USWCicrcleLink.server.user.domain.ExistingMember.ClubMemberTemp;
import com.USWCicrcleLink.server.user.domain.User;
import com.USWCicrcleLink.server.user.repository.ClubMemberAccountStatusRepository;
import com.USWCicrcleLink.server.user.repository.ClubMemberTempRepository;
import com.USWCicrcleLink.server.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ClubLeaderService {
    private final ClubRepository clubRepository;
    private final ClubIntroRepository clubIntroRepository;
    private final ClubMembersRepository clubMembersRepository;
    private final ClubApplicationRepository clubApplicationRepository;
    private final ClubIntroPhotoRepository clubIntroPhotoRepository;
    private final ClubMainPhotoRepository clubMainPhotoRepository;
    private final ProfileRepository profileRepository;
    private final ClubHashtagRepository clubHashtagRepository;
    private final ClubCategoryRepository clubCategoryRepository;
    private final ClubCategoryMappingRepository clubCategoryMappingRepository;
    private final S3FileUploadService s3FileUploadService;
    private final FcmServiceImpl fcmService;
    private final LeaderRepository leaderRepository;
    private final ClubMemberAccountStatusRepository clubMemberAccountStatusRepository;
    private final ClubMemberTempRepository clubMemberTempRepository;
    private final UserRepository userRepository;
    private final FileSignatureValidator fileSignatureValidator;

    // 최대 사진 순서(업로드, 삭제)
    int PHOTO_LIMIT = 5;

    private final String S3_MAINPHOTO_DIR = "mainPhoto/";
    private final String S3_INTROPHOTO_DIR = "introPhoto/";

    // 동아리 접근 권한 확인
    public Club validateLeaderAccess(UUID clubUUID) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomLeaderDetails leaderDetails = (CustomLeaderDetails) authentication.getPrincipal();
        if (!clubUUID.equals(leaderDetails.getClubUUID())) {
            throw new ClubLeaderException(ExceptionType.CLUB_LEADER_ACCESS_DENIED);
        }

        return clubRepository.findByClubUUID(clubUUID)
                .orElseThrow(() -> new ClubException(ExceptionType.CLUB_NOT_EXISTS));
    }

    // 약관 동의 여부 업데이트
    public void updateAgreedTermsTrue() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication.getPrincipal() instanceof CustomLeaderDetails leaderDetails)) {
            throw new UserException(ExceptionType.USER_NOT_EXISTS);
        }

        Leader leader = leaderDetails.leader();
        leader.setAgreeTerms(true);
        leaderRepository.save(leader);
    }

    // 동아리 기본 정보 조회
    @Transactional(readOnly = true)
    public ApiResponse<ClubInfoResponse> getClubInfo(UUID clubUUID) {

        Club club = validateLeaderAccess(clubUUID);

        // 동아리 메인 사진 조회
        Optional<ClubMainPhoto> clubMainPhoto =
                Optional.ofNullable(clubMainPhotoRepository.findByClub_ClubId(club.getClubId()));

        String mainPhotoUrl = clubMainPhoto.map(
                        photo -> s3FileUploadService.generatePresignedGetUrl(photo.getClubMainPhotoS3Key()))
                .orElse(null);

        // 동아리 해시태그 조회
        List<String> clubHashtags = clubHashtagRepository.findByClubClubId(club.getClubId())
                .stream().map(ClubHashtag::getClubHashtag).collect(Collectors.toList());

        // 동아리 카테고리 조회
        List<String> clubCategories = clubCategoryMappingRepository.findByClubClubId(club.getClubId())
                .stream().map(mapping -> mapping.getClubCategory().getClubCategoryName()).collect(Collectors.toList());

        return new ApiResponse<>("동아리 기본 정보 조회 완료",
                new ClubInfoResponse(mainPhotoUrl, club, clubHashtags, clubCategories));
    }

    /**
     * 동아리 기본 정보 변경
     */
    public ApiResponse<UpdateClubInfoResponse> updateClubInfo(UUID clubUUID, ClubInfoRequest clubInfoRequest, MultipartFile mainPhoto) throws IOException {
        // 동아리 회장 유효성 검증
        Club club = validateLeaderAccess(clubUUID);

        // 동아리 회장 이름 변경 시 약관 동의 갱신 필요
        updateLeaderAgreementIfNameChanged(club, clubInfoRequest.getLeaderName());

        // 해시태그 업데이트
        updateClubHashtags(club, clubInfoRequest.getClubHashtag());

        // 카테고리 업데이트
        updateClubCategories(club, clubInfoRequest.getClubCategoryName());

        // 사진 업데이트
        String mainPhotoUrl = updateClubMainPhoto(club.getClubId(), mainPhoto);

        club.updateClubInfo(clubInfoRequest.getLeaderName(), clubInfoRequest.getLeaderHp(), clubInfoRequest.getClubInsta(), clubInfoRequest.getClubRoomNumber());
        log.info("동아리 기본 정보 변경 완료 - Club UUID: {}, Club Name: {}", club.getClubUUID(), club.getClubName());

        return new ApiResponse<>("동아리 기본 정보 변경 완료", new UpdateClubInfoResponse(mainPhotoUrl));
    }

    // 동아리 회장 이름 변경 시 약관 동의 갱신
    private void updateLeaderAgreementIfNameChanged(Club club, String newLeaderName) {

        if (!Objects.equals(club.getLeaderName(), newLeaderName)) {
            Leader leader = leaderRepository.findByClubUUID(club.getClubUUID())
                    .orElseThrow(() -> new ClubLeaderException(ExceptionType.CLUB_LEADER_NOT_EXISTS));

            leader.setAgreeTerms(false);
            leaderRepository.save(leader);
            log.debug("회장 이름 변경으로 약관 동의 상태 초기화 - Leader ID: {}", leader.getLeaderId());
        }
    }

    // 동아리 해시태그 업데이트
    private void updateClubHashtags(Club club, List<String> newHashtags) {
        if (newHashtags == null || newHashtags.isEmpty()) return;

        Set<String> newHashtagsSet = new HashSet<>(newHashtags);
        List<String> existingHashtags = clubHashtagRepository.findHashtagsByClubId(club.getClubId());

        clubHashtagRepository.deleteAllByClub_ClubIdAndClubHashtagNotIn(club.getClubId(), newHashtagsSet);

        // 새 해시태그 중 추가할 항목만 필터링하여 일괄 삽입
        List<ClubHashtag> newHashtagsToInsert = newHashtagsSet.stream()
                .filter(newHashtag -> !existingHashtags.contains(newHashtag))
                .map(newHashtag -> ClubHashtag.builder().club(club).clubHashtag(newHashtag).build())
                .toList();

        clubHashtagRepository.saveAll(newHashtagsToInsert);
    }

    // 동아리 카테고리 업데이트
    private void updateClubCategories(Club club, List<String> newCategories) {
        if (newCategories == null || newCategories.isEmpty()) return;

        Set<String> newCategoriesSet = new HashSet<>(newCategories);
        List<ClubCategoryMapping> existingMappings = clubCategoryMappingRepository.findByClub_ClubId(club.getClubId());
        Set<String> existingCategoryNames = existingMappings.stream()
                .map(mapping -> mapping.getClubCategory().getClubCategoryName())
                .collect(Collectors.toSet());

        clubCategoryMappingRepository.deleteAllByClub_ClubIdAndClubCategory_ClubCategoryNameNotIn(club.getClubId(), newCategoriesSet);

        // 새 카테고리 중 추가할 항목만 필터링하여 일괄 삽입
        List<ClubCategoryMapping> newMappings = newCategoriesSet.stream()
                .filter(categoryName -> !existingCategoryNames.contains(categoryName))
                .map(categoryName -> {
                    ClubCategory clubCategory = clubCategoryRepository.findByClubCategoryName(categoryName)
                            .orElseThrow(() -> new ClubException(ExceptionType.CATEGORY_NOT_FOUND));
                    return ClubCategoryMapping.builder().club(club).clubCategory(clubCategory).build();
                })
                .toList();

        clubCategoryMappingRepository.saveAll(newMappings);
    }

    // 동아리 메인 사진 업데이트
    private String updateClubMainPhoto(Long clubId, MultipartFile mainPhoto) throws IOException {
        if (clubId == null) {
            throw new ClubPhotoException(ExceptionType.CLUB_ID_NOT_EXISTS);
        }

        if (mainPhoto == null || mainPhoto.isEmpty()) {
            return clubMainPhotoRepository.findS3KeyByClubId(clubId).orElse(null);
        }

        return processClubMainPhoto(clubId, mainPhoto);
    }

    // 기존 대표 사진 삭제 및 새로운 파일 업로드
    private String processClubMainPhoto(Long clubId, MultipartFile mainPhoto) throws IOException {
        clubMainPhotoRepository.findS3KeyByClubId(clubId).ifPresent(s3FileUploadService::deleteFile);

        return saveClubMainPhoto(mainPhoto, clubId);
    }

    // 사진 메타데이터 업데이트 및 S3 업로드
    private String saveClubMainPhoto(MultipartFile mainPhoto, Long clubId) throws IOException {
        if (clubId == null) {
            throw new ClubPhotoException(ExceptionType.CLUB_ID_NOT_EXISTS);
        }

        if (mainPhoto == null || mainPhoto.isEmpty()) {
            throw new ClubPhotoException(ExceptionType.CLUB_MAINPHOTO_NOT_EXISTS);
        }

        S3FileResponse s3FileResponse = s3FileUploadService.uploadFile(mainPhoto, S3_MAINPHOTO_DIR);

        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ClubException(ExceptionType.CLUB_NOT_EXISTS));

        Optional<ClubMainPhoto> existingPhoto = clubMainPhotoRepository.findByClub(club);

        existingPhoto.ifPresent(photo -> {
            clubMainPhotoRepository.delete(photo);
            clubMainPhotoRepository.flush();
        });

        ClubMainPhoto clubMainPhoto = ClubMainPhoto.builder()
                .club(club)
                .clubMainPhotoName(mainPhoto.getOriginalFilename())
                .clubMainPhotoS3Key(s3FileResponse.getS3FileName())
                .build();

        clubMainPhotoRepository.save(clubMainPhoto);

        return s3FileResponse.getPresignedUrl();
    }

    // 동아리 요약 조회
    @Transactional(readOnly = true)
    public ClubSummaryResponse getClubSummary(UUID clubUUID) {
        Club club = validateLeaderAccess(clubUUID);

        // 동아리 소개 조회
        ClubIntro clubIntro = clubIntroRepository.findByClubClubId(club.getClubId())
                .orElseThrow(() -> new ClubException(ExceptionType.CLUB_INTRO_NOT_EXISTS));

        // clubHashtag 조회
        List<String> clubHashtags = clubHashtagRepository.findByClubClubId(club.getClubId())
                .stream().map(ClubHashtag::getClubHashtag).toList();

        // 동아리 카테고리 조회
        List<String> clubCategories = clubCategoryMappingRepository.findByClubClubId(club.getClubId())
                .stream().map(mapping -> mapping.getClubCategory().getClubCategoryName()).toList();

        // 동아리 메인 사진 조회
        ClubMainPhoto clubMainPhoto = clubMainPhotoRepository.findByClub(club).orElse(null);

        // 동아리 소개 사진 조회
        List<ClubIntroPhoto> clubIntroPhotos = clubIntroPhotoRepository.findByClubIntro(clubIntro);

        // S3에서 메인 사진 URL 생성 (기본 URL 또는 null 처리)
        String mainPhotoUrl = (clubMainPhoto != null)
                ? s3FileUploadService.generatePresignedGetUrl(clubMainPhoto.getClubMainPhotoS3Key())
                : null;

        // S3에서 소개 사진 URL 생성 (소개 사진이 없을 경우 빈 리스트)
        List<String> introPhotoUrls = clubIntroPhotos.isEmpty()
                ? Collections.emptyList()
                : clubIntroPhotos.stream()
                .sorted(Comparator.comparingInt(ClubIntroPhoto::getOrder))
                .map(photo -> s3FileUploadService.generatePresignedGetUrl(photo.getClubIntroPhotoS3Key()))
                .collect(Collectors.toList());

        return new ClubSummaryResponse(club, clubHashtags, clubCategories, clubIntro, mainPhotoUrl, introPhotoUrls);
    }

    // 동아리 소개 조회
    @Transactional(readOnly = true)
    public ApiResponse<LeaderClubIntroResponse> getClubIntro(UUID clubUUID) {
        Club club = validateLeaderAccess(clubUUID);

        // 동아리 소개 조회
        ClubIntro clubIntro = clubIntroRepository.findByClubClubId(club.getClubId())
                .orElseThrow(() -> new ClubException(ExceptionType.CLUB_INTRO_NOT_EXISTS));

        // 동아리 소개 사진 조회
        List<ClubIntroPhoto> clubIntroPhotos = clubIntroPhotoRepository.findByClubIntro(clubIntro);

        // S3에서 소개 사진 URL 생성 (소개 사진이 없을 경우 빈 리스트)
        List<String> introPhotoUrls = clubIntroPhotos.isEmpty()
                ? Collections.emptyList()
                : clubIntroPhotos.stream()
                .sorted(Comparator.comparingInt(ClubIntroPhoto::getOrder))
                .map(photo -> s3FileUploadService.generatePresignedGetUrl(photo.getClubIntroPhotoS3Key()))
                .collect(Collectors.toList());

        return new ApiResponse<>("동아리 소개 조회 완료", new LeaderClubIntroResponse(club, clubIntro, introPhotoUrls));
    }

    // 동아리 소개 변경
    public ApiResponse updateClubIntro(UUID clubUUID, ClubIntroRequest clubIntroRequest, List<MultipartFile> introPhotos) throws IOException {

        Club club = validateLeaderAccess(clubUUID);

        ClubIntro clubIntro = clubIntroRepository.findByClubClubId(club.getClubId())
                .orElseThrow(() -> new ClubException(ExceptionType.CLUB_INTRO_NOT_EXISTS));

        // 모집 상태가 null일 때 예외 처리
        if (clubIntroRequest.getRecruitmentStatus() == null) {
            throw new ClubException(ExceptionType.INVALID_RECRUITMENT_STATUS);
        }

        // 삭제할 사진 확인
        if (clubIntroRequest.getDeletedOrders() != null && !clubIntroRequest.getDeletedOrders().isEmpty()) {
            // 순서 개수, 범위 검증
            validateOrderValues(clubIntroRequest.getDeletedOrders());

            for (int i = 0; i < clubIntroRequest.getDeletedOrders().size(); i++) {// 하나씩 삭제
                int deletingOrder = clubIntroRequest.getDeletedOrders().get(i);

                ClubIntroPhoto deletingPhoto = clubIntroPhotoRepository
                        .findByClubIntro_ClubIntroIdAndOrder(clubIntro.getClubIntroId(), deletingOrder)
                        .orElseThrow(() -> new ClubPhotoException(ExceptionType.PHOTO_ORDER_MISS_MATCH));

                s3FileUploadService.deleteFile(deletingPhoto.getClubIntroPhotoS3Key());

                deletingPhoto.updateClubIntroPhoto("", "", deletingOrder);
                clubIntroPhotoRepository.save(deletingPhoto);

                log.debug("소개 사진 삭제 완료: {}", deletingPhoto.getOrder());
            }
        }

        // 각 사진의 조회 presignedUrls
        List<String> presignedUrls = new ArrayList<>();

        // 동아리 소개 사진을 넣을 경우
        if (introPhotos != null && !introPhotos.isEmpty() && clubIntroRequest.getOrders() != null && !clubIntroRequest.getOrders().isEmpty()) {

            // 순서 개수, 범위 검증
            validateOrderValues(clubIntroRequest.getOrders());

            if (introPhotos.size() > PHOTO_LIMIT) {// 최대 5장 업로드
                throw new FileException(ExceptionType.MAXIMUM_FILE_LIMIT_EXCEEDED);
            }

            // N번째 사진 1장씩
            for (int i = 0; i < introPhotos.size(); i++) {
                MultipartFile introPhoto = introPhotos.get(i);
                int order = clubIntroRequest.getOrders().get(i);

                // 동아리 소개 사진이 존재하지 않으면 순서 스킵
                if (introPhoto == null || introPhoto.isEmpty()) {
                    continue;
                }

                ClubIntroPhoto existingPhoto = clubIntroPhotoRepository
                        .findByClubIntro_ClubIntroIdAndOrder(clubIntro.getClubIntroId(), order)
                        .orElseThrow(() -> new ClubPhotoException(ExceptionType.PHOTO_ORDER_MISS_MATCH));

                S3FileResponse s3FileResponse;

                // N번째 동아리 소개 사진 존재할 경우
                if (!Optional.ofNullable(existingPhoto.getClubIntroPhotoName()).orElse("").isEmpty() &&
                        !Optional.ofNullable(existingPhoto.getClubIntroPhotoS3Key()).orElse("").isEmpty()) {

                    // 기존 S3 파일 삭제
                    s3FileUploadService.deleteFile(existingPhoto.getClubIntroPhotoS3Key());
                    log.debug("기존 소개 사진 삭제 완료: {}", existingPhoto.getClubIntroPhotoS3Key());
                }
                // 새로운 파일 업로드 및 메타 데이터 업데이트
                s3FileResponse = updateClubIntroPhotoAndS3File(introPhoto, existingPhoto, order);

                // 업로드된 사진의 사전 서명된 URL을 리스트에 추가
                presignedUrls.add(s3FileResponse.getPresignedUrl());
            }
        }

        // 소개 글, 모집 글, google form 저장
        clubIntro.updateClubIntro(clubIntroRequest.getClubIntro(), clubIntroRequest.getClubRecruitment(), clubIntroRequest.getGoogleFormUrl());
        clubIntroRepository.save(clubIntro);

        log.debug("{} 동아리 소개 변경 완료", club.getClubName());
        return new ApiResponse<>("동아리 소개 변경 완료", new UpdateClubIntroResponse(presignedUrls));
    }

    private void validateOrderValues(List<Integer> orders) {
        // 순서 개수 체크
        if (orders.size() < 1 || orders.size() > PHOTO_LIMIT) {// 0 이하 6이상
            throw new ClubPhotoException(ExceptionType.PHOTO_ORDER_MISS_MATCH);
        }

        // 순서 값
        for (int order : orders) {
            if (order < 1 || order > PHOTO_LIMIT) { // 1 ~ 5 사이여야 함
                throw new ClubPhotoException(ExceptionType.PHOTO_ORDER_MISS_MATCH);
            }
        }

    }

    private S3FileResponse updateClubIntroPhotoAndS3File(MultipartFile introPhoto, ClubIntroPhoto existingPhoto, int order) throws IOException {
        // 새로운 파일 업로드
        S3FileResponse s3FileResponse = s3FileUploadService.uploadFile(introPhoto, S3_INTROPHOTO_DIR);

        // null 체크 후 값 설정
        String newPhotoName = introPhoto.getOriginalFilename() != null ? introPhoto.getOriginalFilename() : "";
        String newS3Key = s3FileResponse.getS3FileName() != null ? s3FileResponse.getS3FileName() : "";

        // s3key 및 photoname 업데이트
        existingPhoto.updateClubIntroPhoto(newPhotoName, newS3Key, order);
        clubIntroPhotoRepository.save(existingPhoto);
        log.debug("사진 정보 저장 및 업데이트 완료: {}", s3FileResponse.getS3FileName());

        return s3FileResponse;
    }

    // 동아리 모집 상태 변경
    public ApiResponse toggleRecruitmentStatus(UUID clubUUID) {

        Club club = validateLeaderAccess(clubUUID);

        ClubIntro clubIntro = clubIntroRepository.findByClubClubId(club.getClubId())
                .orElseThrow(() -> new ClubException(ExceptionType.CLUB_INTRO_NOT_EXISTS));
        log.debug("동아리 소개 조회 결과: {}", clubIntro);

        // 모집 상태 현재와 반전
        clubIntro.toggleRecruitmentStatus();
        clubRepository.save(club);

        return new ApiResponse<>("동아리 모집 상태 변경 완료", clubIntro.getRecruitmentStatus());
    }

    // 소속 동아리원 조회(구, 성능 비교용)
//    @Transactional(readOnly = true)
//    public ApiResponse<List<ClubMembersResponse>> findClubMembers(LeaderToken token) {
//
//        Club club = validateLeaderAccess(token);
//
//        // 해당 동아리원 조회(성능 비교)
////        List<ClubMembers> findClubMembers = clubMembersRepository.findByClub(club); // 일반
//        List<ClubMembers> findClubMembers = clubMembersRepository.findAllWithProfileByClubClubId(club.getClubId()); // 성능
//
//        // 동아리원과 프로필 조회
//        List<ClubMembersResponse> memberProfiles = findClubMembers.stream()
//                .map(cm -> new ClubMembersResponse(
//                        cm.getClubMemberId(),
//                        cm.getProfile()
//                ))
//                .collect(toList());
//
//        return new ApiResponse<>("소속 동아리원 조회 완료", memberProfiles);
//    }

    // 소속 동아리 회원 조회(가나다순 정렬)
    @Transactional(readOnly = true)
    public ApiResponse<List<ClubMembersResponse>> getClubMembers(UUID clubUUID) {

        Club club = validateLeaderAccess(clubUUID);

        List<ClubMembers> findClubMembers = clubMembersRepository.findAllWithProfileByName(club.getClubId());

        // 동아리원과 프로필 조회
        List<ClubMembersResponse> memberProfiles = findClubMembers.stream()
                .map(cm -> new ClubMembersResponse(
                        cm.getClubMemberUUID(),
                        cm.getProfile()
                ))
                .collect(toList());

        return new ApiResponse<>("소속 동아리 회원 가나다순 조회 완료", memberProfiles);
    }

    // 소속 동아리 회원 조회(정회원/ 비회원 정렬)
    @Transactional(readOnly = true)
    public ApiResponse<List<ClubMembersResponse>> getClubMembersByMemberType(UUID clubUUID, MemberType memberType) {

        Club club = validateLeaderAccess(clubUUID);

        List<ClubMembers> findClubMembers = clubMembersRepository.findAllWithProfileByMemberType(club.getClubId(), memberType);

        // 동아리원과 프로필 조회
        List<ClubMembersResponse> memberProfiles = findClubMembers.stream()
                .map(cm -> new ClubMembersResponse(
                        cm.getClubMemberUUID(),
                        cm.getProfile()
                ))
                .collect(toList());

        // memberType에 따라 메시지 변경
        String message = switch (memberType) {
            case REGULARMEMBER -> "소속 동아리 정회원 조회 완료";
            case NONMEMBER -> "소속 동아리 비회원 조회 완료";
        };

        return new ApiResponse<>(message, memberProfiles);
    }

    // 소속 동아리원 삭제
    public ApiResponse deleteClubMembers(UUID clubUUID, List<ClubMembersDeleteRequest> clubMemberUUIDList) {

        Club club = validateLeaderAccess(clubUUID);

        List<UUID> clubMemberUUIDs = clubMemberUUIDList.stream()
                .map(ClubMembersDeleteRequest::getClubMemberUUID)
                .toList();

        // 동아리 회원인지 확인
        List<ClubMembers> membersToDelete = clubMembersRepository.findByClubClubIdAndClubMemberUUIDIn(club.getClubId(), clubMemberUUIDs);

        // 조회된 수와 요청한 수와 같은지(다르면 다른 동아리 회원이 존재)
        if (membersToDelete.size() != clubMemberUUIDList.size()) {
            throw new ClubMemberException(ExceptionType.CLUB_MEMBER_NOT_EXISTS);
        }

        // 동아리 회원 삭제
        clubMembersRepository.deleteAll(membersToDelete);

        // 삭제 후 비회원이면서 어떤 동아리에도 소속돼 있지 않을 경우, 프로필 삭제
        List<Long> profileIdsToDelete = membersToDelete.stream()
                .map(ClubMembers::getProfile)
                .filter(profile -> profile.getMemberType() == MemberType.NONMEMBER)
                .map(Profile::getProfileId)
                .toList();

        List<Long> profileIdsWithoutClub = clubMembersRepository.findByProfileProfileIdsWithoutClub(profileIdsToDelete);

        if (!profileIdsWithoutClub.isEmpty()) {// 삭제할 회원이 존재할 경우
            // 삭제할 id가 있는 경우
            profileRepository.deleteAllByIdInBatch(profileIdsWithoutClub);
        }

        return new ApiResponse<>("동아리 회원 삭제 완료", clubMemberUUIDList);
    }

    // 소속 동아리원 엑셀 다운
    @Transactional(readOnly = true)
    public void downloadExcel(UUID clubUUID, HttpServletResponse response) {

        Club club = validateLeaderAccess(clubUUID);

        // 해당 동아리원 조회
        List<ClubMembers> findClubMembers = clubMembersRepository.findAllWithProfileByClubClubId(club.getClubId());

        // 동아리원의 프로필 조회 후 동아리원 정보로 정리
        List<ClubMembersExportExcelResponse> memberProfiles = findClubMembers.stream()
                .map(cm -> new ClubMembersExportExcelResponse(
                        cm.getProfile()
                ))
                .toList();

        // 파일 이름 설정
        String fileName = club.getClubName() + "_회원_명단.xlsx";
        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, "UTF-8");
        } catch (IOException e) {
            throw new FileException(ExceptionType.FILE_ENCODING_FAILED);
        }

        // Content-Disposition 헤더 설정
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + encodedFileName);

        // 엑셀 파일 생성
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ) {
            // 시트 이름 설정
            Sheet sheet = workbook.createSheet(club.getClubName());

            // 표 바탕색
            CellStyle blueCellStyle = workbook.createCellStyle();
            applyCellStyle(blueCellStyle, new Color(74, 119, 202));

            // 표 시작 위치
            Row headerRow = sheet.createRow(0);

            // 카테고리 설정
            String[] columnHeaders = {"학과", "학번", "이름", "전화번호"};
            for (int i = 0; i < columnHeaders.length; i++) {// 셀 생성, 카테고리 부여
                Cell headerCell = headerRow.createCell(i);
                headerCell.setCellValue(columnHeaders[i]);
                headerCell.setCellStyle(blueCellStyle);
            }

            // DB값 엑셀 파일에 넣기
            int rowNum = 1;
            for (ClubMembersExportExcelResponse member : memberProfiles) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(member.getMajor());
                row.createCell(1).setCellValue(member.getStudentNumber());
                row.createCell(2).setCellValue(member.getUserName());
                row.createCell(3).setCellValue(member.getUserHp());
            }

            workbook.write(outputStream);
            outputStream.writeTo(response.getOutputStream());
            response.flushBuffer();
            log.debug("{} 파일 추출 완료", club.getClubName());
        } catch (IOException e) {
            throw new FileException(ExceptionType.FILE_CREATE_FAILED);
        }
    }

    // 엑셀 표 스타일 설정
    private void applyCellStyle(CellStyle cellStyle, Color color) {
        XSSFCellStyle xssfCellStyle = (XSSFCellStyle) cellStyle;
        xssfCellStyle.setFillForegroundColor(new XSSFColor(color, new DefaultIndexedColorMap()));
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // 글 정렬
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 표 그리기
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setBorderBottom(BorderStyle.THIN);
    }

    // 동아리 지원자 조회
    @Transactional(readOnly = true)
    public ApiResponse<List<ApplicantsResponse>> getApplicants(UUID clubUUID) {
        Club club = validateLeaderAccess(clubUUID);

        // 합/불 처리되지 않은 동아리 지원자 조회
        List<ClubApplication> clubApplications = clubApplicationRepository.findAllWithProfileByClubId(club.getClubId(), false);
        List<ApplicantsResponse> applicants = clubApplications.stream()
                .map(ap -> new ApplicantsResponse(
                        ap.getClubApplicationUUID(),
                        ap.getProfile()
                ))
                .toList();

        return new ApiResponse<>("최초 동아리 지원자 조회 완료", applicants);
    }

    // 최초 합격자 알림
    public void updateApplicantResults(UUID clubUUID, List<ApplicantResultsRequest> results) throws IOException {
        Club club = validateLeaderAccess(clubUUID);

        // 동아리 지원자 전원 조회(최초 합격)
        List<ClubApplication> applicants = clubApplicationRepository.findByClub_ClubIdAndChecked(club.getClubId(), false);

        // 선택된 지원자 수와 전체 동아리 지원자 수 비교
        validateTotalApplicants(applicants, results);

        // 지원자 검증(지원한 동아리 + 지원서 + check안된 상태)
        for (ApplicantResultsRequest result : results) {
            ClubApplication applicant = clubApplicationRepository.findByClub_ClubIdAndClubApplicationUUIDAndChecked(
                            club.getClubId(),
                            result.getClubApplicationUUID(),
                            false)
                    .orElseThrow(() -> new ClubApplicationException(ExceptionType.APPLICANT_NOT_EXISTS));

            // 중복 가입 체크
            checkDuplicateClubMember(applicant.getProfile().getProfileId(), club.getClubId());

            // 합격 불합격 상태 업데이트
            // 합/불, checked, 삭제 날짜
            ClubApplicationStatus clubApplicationResult = result.getClubApplicationStatus();// 지원 결과 PASS/ FAIL
            if (clubApplicationResult == ClubApplicationStatus.PASS) {
                ClubMembers newClubMembers = ClubMembers.builder()
                        .club(club)
                        .profile(applicant.getProfile())
                        .build();
                applicant.updateClubApplicationStatus(clubApplicationResult, true, LocalDateTime.now().plusDays(4));
                clubMembersRepository.save(newClubMembers);
                log.debug("합격 처리 완료: {}", applicant.getClubApplicationUUID());
            } else if (clubApplicationResult == ClubApplicationStatus.FAIL) {
                applicant.updateClubApplicationStatus(clubApplicationResult, true, LocalDateTime.now().plusDays(4));
                log.debug("불합격 처리 완료: {}", applicant.getClubApplicationUUID());
            }

            clubApplicationRepository.save(applicant);
            fcmService.sendMessageTo(applicant, clubApplicationResult);
        }
    }

    // 동아리 회원 중복 검사
    private void checkDuplicateClubMember(Long profileId, Long clubId) {
        boolean isDuplicate = clubMembersRepository
                .findByProfileProfileIdAndClubClubId(profileId, clubId)
                .isPresent();

        if (isDuplicate) {
            throw new ClubMemberException(ExceptionType.CLUB_MEMBER_ALREADY_EXISTS);
        }
    }

    // 선택된 지원자 수와 전체 지원자 수 비교
    private void validateTotalApplicants(List<ClubApplication> applicants, List<ApplicantResultsRequest> results) {
        Set<UUID> applicantUUIDs = applicants.stream()
                .map(ClubApplication::getClubApplicationUUID)
                .collect(Collectors.toSet());

        Set<UUID> requestedApplicantUUIDs = results.stream()
                .map(ApplicantResultsRequest::getClubApplicationUUID)
                .collect(Collectors.toSet());

        if (!requestedApplicantUUIDs.equals(applicantUUIDs)) {
            throw new ClubApplicationException(ExceptionType.APPLICANT_COUNT_MISMATCH);
        }
    }

    // 불합격자 조회
    @Transactional(readOnly = true)
    public ApiResponse<List<ApplicantsResponse>> getFailedApplicants(UUID clubUUID) {
        Club club = validateLeaderAccess(clubUUID);

        // 불합격자 동아리 지원자 조회
        List<ClubApplication> clubApplications = clubApplicationRepository.findAllWithProfileByClubIdAndFailed(club.getClubId(), true, ClubApplicationStatus.FAIL);
        List<ApplicantsResponse> applicants = clubApplications.stream()
                .map(ap -> new ApplicantsResponse(
                        ap.getClubApplicationUUID(),
                        ap.getProfile()
                ))
                .toList();

        return new ApiResponse<>("불합격자 조회 완료", applicants);
    }

    // 동아리 지원자 추가 합격 처리
    public void updateFailedApplicantResults(UUID clubUUID, List<ApplicantResultsRequest> results) throws IOException {
        Club club = validateLeaderAccess(clubUUID);

        // 지원자 검증(지원한 동아리 + 지원서 + check된 상태 + 불합)
        for (ApplicantResultsRequest result : results) {
            ClubApplication applicant = clubApplicationRepository.findByClub_ClubIdAndClubApplicationUUIDAndCheckedAndClubApplicationStatus(
                            club.getClubId(),
                            result.getClubApplicationUUID(),
                            true,
                            ClubApplicationStatus.FAIL
                    )
                    .orElseThrow(() -> new ClubApplicationException(ExceptionType.ADDITIONAL_APPLICANT_NOT_EXISTS));

            // 중복 가입 체크
            checkDuplicateClubMember(applicant.getProfile().getProfileId(), club.getClubId());

            // 합격 불합격 상태 업데이트
            // 합격
            ClubMembers newClubMembers = ClubMembers.builder()
                    .club(club)
                    .profile(applicant.getProfile())
                    .build();
            clubMembersRepository.save(newClubMembers);

            ClubApplicationStatus clubApplicationResult = result.getClubApplicationStatus();
            applicant.updateFailedClubApplicationStatus(clubApplicationResult);
            clubApplicationRepository.save(applicant);

            fcmService.sendMessageTo(applicant, clubApplicationResult);
            log.debug("추가 합격 처리 완료: {}", applicant.getClubApplicationUUID());
        }
    }


    // 기존 동아리원 가져오기(엑셀 파일)
    @Transactional(readOnly = true)
    public ApiResponse<ClubMembersImportExcelResponse> uploadExcel(UUID clubUUID, MultipartFile clubMembersFile) throws IOException {
        validateLeaderAccess(clubUUID);

        // 엑셀 파일의 개수 확인
        if (clubMembersFile == null || clubMembersFile.isEmpty()) {
            throw new FileException(ExceptionType.MAXIMUM_FILE_LIMIT_EXCEEDED);
        }

        // 파일 확장자 확인
        String fileExtension = validateClubMembersExcelFile(clubMembersFile);

        // 엑셀 파일 확장자(구, 신버전)
        Workbook workbook;
        if (fileExtension.equals("xls")) {// 엑셀 버전 ~03
            workbook = new HSSFWorkbook(clubMembersFile.getInputStream());
        } else {// 엑셀 버전 07~
            workbook = new XSSFWorkbook(clubMembersFile.getInputStream());
        }

        Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트 사용

        // 추가 동아리 회원
        List<ExcelProfileMemberResponse> addClubMembers = new ArrayList<>();
        // 중복 동아리 회원
        List<ExcelProfileMemberResponse> duplicateClubMembers = new ArrayList<>();

        // 엑셀 데이터를 읽어 이름, 학번, 전화번호 수집
        Set<String> userNames = new HashSet<>();
        Set<String> studentNumbers = new HashSet<>();
        Set<String> userHpNumbers = new HashSet<>();
        // 이름_학번_전화번호를 키로 원본 데이터 저장
        Map<String, ClubMemberExcelDataDto> rowExcelDataMap = new HashMap<>();

        // 엑셀 파일 읽기
        for (int i = 1; i <= sheet.getLastRowNum(); i++) { // 첫 번째 행(헤더) 건너뛰기
            Row row = sheet.getRow(i);
            // 빈 줄은 무시
            if (row == null || isRowEmpty(row)) {
                continue;
            }

            // 셀 읽기
            String userName = getCellValue(row.getCell(0)).replaceAll("\\s+", ""); // 이름
            String studentNumber = getCellValue(row.getCell(1)).replaceAll("\\s+", ""); // 학번
            String userHp = getCellValue(row.getCell(2)).replaceAll("-", "").replaceAll("\\s+", ""); // 전화번호

            // 데이터 수집 후 한번에 조회
            userNames.add(userName);
            studentNumbers.add(studentNumber);
            userHpNumbers.add(userHp);
            rowExcelDataMap.put(userName + "_" + studentNumber + "_" + userHp, new ClubMemberExcelDataDto(userName, studentNumber, userHp));
        }

        // DB에서 중복 데이터 한 번에 확인
        List<Profile> duplicateProfiles = profileRepository.findByUserNameInAndStudentNumberInAndUserHpIn(userNames, studentNumbers, userHpNumbers);

        // 중복 데이터 매핑
        for (Profile profile : duplicateProfiles) {
            // 엑셀 데이터를 기반으로 매핑
            String duplicateProfileKey = profile.getUserName() + "_" + profile.getStudentNumber() + "_" + profile.getUserHp();// key
            ClubMemberExcelDataDto duplicateExcelData = rowExcelDataMap.get(duplicateProfileKey);// map 검색
            if (duplicateExcelData != null) {
                duplicateClubMembers.add(new ExcelProfileMemberResponse(
                        profile.getUserName(),
                        profile.getStudentNumber(),
                        profile.getUserHp()
                ));
            }
            // 확인한 key:value 삭제
            rowExcelDataMap.remove(duplicateProfileKey);
        }

        // 중복이 아닌 데이터 추가
        for (ClubMemberExcelDataDto addClubMember : rowExcelDataMap.values()) {
            addClubMembers.add(new ExcelProfileMemberResponse(
                    addClubMember.getUserName(),
                    addClubMember.getStudentNumber(),
                    addClubMember.getUserHp()
            ));
        }

        ClubMembersImportExcelResponse response = new ClubMembersImportExcelResponse(addClubMembers, duplicateClubMembers);
        return new ApiResponse<>("기존 동아리 회원 엑셀로 가져오기 완료", response);
    }

    private String validateClubMembersExcelFile(MultipartFile clubMembersFile) {
        // 파일 확장자 확인
        String fileExtension = FilenameUtils.getExtension(clubMembersFile.getOriginalFilename());
        if (!fileExtension.equals("xls") && !fileExtension.equals("xlsx")) {
            throw new FileException(ExceptionType.UNSUPPORTED_FILE_EXTENSION);
        }

        // 파일 시그니처를 통해 실제 파일 형식이 올바른지 확인
        try {
            if (!fileSignatureValidator.isValidFileType(clubMembersFile.getInputStream(), fileExtension)) {
                throw new FileException(ExceptionType.UNSUPPORTED_FILE_EXTENSION);
            }
        } catch (IOException e) {
            throw new FileException(ExceptionType.FILE_VALIDATION_FAILED);
        }

        return fileExtension;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return "";
    }

    private boolean isRowEmpty(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }

    // 기존 동아리원 추가(엑셀)
    public void addClubMembersFromExcel(UUID clubUUID, List<ClubMembersAddFromExcelRequest> clubMembersAddFromExcelRequests) {
        Club club = validateLeaderAccess(clubUUID);

        // 중복 확인 데이터 수집
        Map<String, ClubMembersAddFromExcelRequest> requestDataMap = new HashMap<>();
        List<Map<String, String>> duplicateUsers = new ArrayList<>();

        // 요청 데이터를 키로 매핑 (이름_학번_전화번호_전공)
        for (ClubMembersAddFromExcelRequest request : clubMembersAddFromExcelRequests) {
            if (request.getMajor() == null || request.getMajor().trim().isEmpty()) {
                throw new ProfileException(ExceptionType.DEPARTMENT_NOT_INPUT);
            }

            String clubMemberKey = request.getUserName() + "_"
                    + request.getStudentNumber() + "_"
                    + request.getUserHp() + "_"
                    + request.getMajor();
            requestDataMap.put(clubMemberKey, request);
        }

        // DB에서 중복 데이터 확인 (이름, 학번, 전화번호, 전공을 모두 포함)
        List<Profile> duplicateProfiles = profileRepository.findByUserNameInAndStudentNumberInAndUserHpInAndMajorIn(
                requestDataMap.values().stream().map(ClubMembersAddFromExcelRequest::getUserName).collect(Collectors.toSet()),
                requestDataMap.values().stream().map(ClubMembersAddFromExcelRequest::getStudentNumber).collect(Collectors.toSet()),
                requestDataMap.values().stream().map(ClubMembersAddFromExcelRequest::getUserHp).collect(Collectors.toSet()),
                requestDataMap.values().stream().map(ClubMembersAddFromExcelRequest::getMajor).collect(Collectors.toSet())
        );

        // 중복 확인 및 매핑
        for (Profile profile : duplicateProfiles) {
            String uniqueKey = profile.getUserName() + "_" + profile.getStudentNumber() + "_" + profile.getUserHp() + "_" + profile.getMajor();

            ClubMembersAddFromExcelRequest duplicateRequest = requestDataMap.get(uniqueKey);
            if (duplicateRequest != null) {
                duplicateUsers.add(Map.of(
                        "이름", profile.getUserName(),
                        "학번", profile.getStudentNumber(),
                        "전화번호", profile.getUserHp(),
                        "전공", profile.getMajor()
                ));
                requestDataMap.remove(uniqueKey); // 중복 데이터는 저장 대상에서 제거
            }
        }

        // 중복된 데이터가 있으면 예외 처리
        if (!duplicateUsers.isEmpty()) {
            throw new ProfileException(ExceptionType.DUPLICATE_PROFILE, duplicateUsers);
        }

        // 중복되지 않은 데이터만 저장
        for (ClubMembersAddFromExcelRequest validRequest : requestDataMap.values()) {
            Profile profile = Profile.builder()
                    .userName(validRequest.getUserName())
                    .studentNumber(validRequest.getStudentNumber())
                    .userHp(validRequest.getUserHp())
                    .major(validRequest.getMajor())
                    .profileCreatedAt(LocalDateTime.now())
                    .profileUpdatedAt(LocalDateTime.now())
                    .memberType(MemberType.NONMEMBER)
                    .build();
            profileRepository.save(profile);

            ClubMembers clubMember = ClubMembers.builder()
                    .club(club)
                    .profile(profile)
                    .build();
            clubMembersRepository.save(clubMember);
        }
    }

    // 프로필 중복 동아리 회원 추가
    public ApiResponse addDuplicateProfileMember(UUID clubUUID, DuplicateProfileMemberRequest duplicateProfileMemberRequest) {
        Club club = validateLeaderAccess(clubUUID);

        // 프로필 중복 회원 조회
        Profile duplicateProfile = profileRepository
                .findByUserNameAndStudentNumberAndUserHp(
                        duplicateProfileMemberRequest.getUserName(),
                        duplicateProfileMemberRequest.getStudentNumber(),
                        duplicateProfileMemberRequest.getUserHp()
                ).orElseThrow(() -> new ProfileException(ExceptionType.PROFILE_NOT_EXISTS));

        // 동아리 회원 중복 검사
        checkDuplicateClubMember(duplicateProfile.getProfileId(), club.getClubId());

        // 존재하면 동아리 회원으로 추가
        ClubMembers duplicateProfileClubMember = ClubMembers.builder()
                .club(club)
                .profile(duplicateProfile)
                .build();

        clubMembersRepository.save(duplicateProfileClubMember);
        return new ApiResponse<>("프로필 중복 동아리 회원 추가 완료", duplicateProfileMemberRequest);
    }

    // 비회원 프로필 업데이트
    public ApiResponse updateNonMemberProfile(UUID clubUUID,
                                              UUID clubMemberUUID,
                                              ClubNonMemberUpdateRequest request) {
        Club club = validateLeaderAccess(clubUUID);

        // 동아리 회원 확인
        ClubMembers clubMember = clubMembersRepository.findByClubClubIdAndClubMemberUUID(club.getClubId(), clubMemberUUID)
                .orElseThrow(() -> new ClubMemberException(ExceptionType.CLUB_MEMBER_NOT_EXISTS));

        // 비회원 확인
        if (clubMember.getProfile().getMemberType() != MemberType.NONMEMBER) {
            throw new ClubMemberException(ExceptionType.NOT_NON_MEMBER);
        }

        // 프로필 업데이트
        Profile profile = clubMember.getProfile();
        profile.updateProfile(request.getUserName(), request.getStudentNumber(), request.getMajor(), request.getUserHp());
        profileRepository.save(profile);

        return new ApiResponse("비회원 프로필 업데이트 완료", request);
    }

    // 기존 동아리 회원 가입 요청 조회
    @Transactional(readOnly = true)
    public ApiResponse getSignUpRequest(UUID clubUUID) {
        Club club = validateLeaderAccess(clubUUID);

        List<ClubMemberAccountStatus> signUpClubMember = clubMemberAccountStatusRepository.findAllWithClubMemberTemp(club.getClubId());
        List<SignUpRequestResponse> signUpRequestResponse = signUpClubMember.stream().map(
                cmt -> new SignUpRequestResponse(
                        cmt.getClubMemberAccountStatusUUID(),
                        cmt.getClubMemberTemp()
                )
        ).toList();

        return new ApiResponse("기존 동아리 회원 가입 요청 조회 완료", signUpRequestResponse);
    }

    // 기존 동아리 회원 가입 요청 삭제
    public ApiResponse deleteSignUpRequest(UUID clubUUID, UUID clubMemberAccountStatusUUID) {
        Club club = validateLeaderAccess(clubUUID);

        // 동아리 + 기존 동아리 회원 가입 요청 확인
        ClubMemberAccountStatus clubMemberAccountStatus = clubMemberAccountStatusRepository.findByClubMemberAccountStatusUUIDAndClub_ClubUUID(clubMemberAccountStatusUUID, club.getClubUUID())
                .orElseThrow(() -> new ClubMemberAccountStatusException(ExceptionType.CLUB_MEMBER_SIGN_UP_REQUEST_NOT_EXISTS));

        clubMemberAccountStatusRepository.delete(clubMemberAccountStatus);
        return new ApiResponse("기존 동아리 회원 가입 요청 거절 완료");
    }

    // 기존 동아리 회원 가입 요청 수락
    public ApiResponse acceptSignUpRequest(UUID clubUUID, ClubMembersAcceptSignUpRequest clubMembersAcceptSignUpRequest) {
        Club club = validateLeaderAccess(clubUUID);

        ClubMemberProfileRequest signUpProfile = clubMembersAcceptSignUpRequest.getSignUpProfileRequest();
        ClubMemberProfileRequest clubNonMemberProfile = clubMembersAcceptSignUpRequest.getClubNonMemberProfileRequest();

        // 요청한 프로필의 존재 여부, 필드 값 유효성 확인
        ClubMemberAccountStatus clubMemberAccountStatus = validateSignUpProfile(signUpProfile, club.getClubId());
        ClubMemberTemp clubMemberTemp = clubMemberAccountStatus.getClubMemberTemp();
        Profile clubNonMember = validateClubNonMemberProfile(clubNonMemberProfile, club.getClubId());

        // 두 프로필 값이 같은지 비교, 예외처리
        compareProfile(signUpProfile, clubNonMemberProfile);

        // 두 프로필이 같으면 동아리 회장 수락 횟수 증가
        clubMemberTemp.updateClubRequestCount();
        clubMemberTempRepository.save(clubMemberTemp);

        //회원 가입 요청 삭제
        clubMemberAccountStatusRepository.delete(clubMemberAccountStatus);

        // 가입 요청 횟수와 동아리 회장 수락 횟수가 같으면 계정 생성
        if (clubMemberTemp.getTotalClubRequest() == clubMemberTemp.getClubRequestCount()) {
            User user = User.builder()
                    .userAccount(clubMemberTemp.getProfileTempAccount())
                    .userPw(clubMemberTemp.getProfileTempPw())
                    .email(clubMemberTemp.getProfileTempEmail())
                    .userCreatedAt(LocalDateTime.now())
                    .userUpdatedAt(LocalDateTime.now())
                    .role(Role.USER)
                    .build();
            userRepository.save(user);

            // 비회원-> 정회원 변경
            clubNonMember.updateMemberType(MemberType.REGULARMEMBER);

            // 비회원의 프로필에 user 넣기(엑셀로 추가한 동아리 회원)
            clubNonMember.updateUser(user);
            profileRepository.save(clubNonMember);

            // 계정 생성 후 필요 없는 테이블 정보 삭제
            clubMemberTempRepository.delete(clubMemberTemp);

            // 계정 생성 시 이름과 반환, 계정 생성된 사람에게 알려주는 팝업
            return new ApiResponse<>("기존 동아리 회원 가입 요청 수락 후 계정 생성 완료", clubNonMember.getUserName());
        }

        return new ApiResponse("기존 동아리 회원 가입 요청 수락 완료");
    }

    // 기존 회원 가입 요청 검증
    private ClubMemberAccountStatus validateSignUpProfile(ClubMemberProfileRequest signUpProfileRequest, Long clubId) {
        ClubMemberAccountStatus clubMemberAccountStatus = clubMemberAccountStatusRepository.findByClubMemberAccountStatusUUIDAndClub_ClubId(signUpProfileRequest.getUuid(), clubId)
                .orElseThrow(() -> new ClubMemberAccountStatusException(ExceptionType.CLUB_MEMBER_SIGN_UP_REQUEST_NOT_EXISTS));

        ClubMemberTemp clubMemberTemp = clubMemberAccountStatus.getClubMemberTemp();

        // 요청의 필드 값과 DB와 비교
        if (!Objects.equals(signUpProfileRequest.getUserName(), clubMemberTemp.getProfileTempName()) ||
                !Objects.equals(signUpProfileRequest.getStudentNumber(), clubMemberTemp.getProfileTempStudentNumber()) ||
                !Objects.equals(signUpProfileRequest.getMajor(), clubMemberTemp.getProfileTempMajor()) ||
                !Objects.equals(signUpProfileRequest.getUserHp(), clubMemberTemp.getProfileTempHp())) {
            throw new ClubMemberAccountStatusException(ExceptionType.CLUB_MEMBER_SIGN_UP_REQUEST_NOT_EXISTS);
        }
        return clubMemberAccountStatus;
    }

    // 기존 비회원 프로필 검증
    private Profile validateClubNonMemberProfile(ClubMemberProfileRequest clubNonMemberProfileRequest, Long clubId) {
        Profile clubNonMember = clubMembersRepository.findByClubClubIdAndClubMemberUUID(clubId, clubNonMemberProfileRequest.getUuid())
                .map(ClubMembers::getProfile)
                .orElseThrow(() -> new ClubMemberException(ExceptionType.CLUB_MEMBER_NOT_EXISTS));

        // 요청의 필드 값과 DB와 비교
        if (!Objects.equals(clubNonMemberProfileRequest.getUserName(), clubNonMember.getUserName()) ||
                !Objects.equals(clubNonMemberProfileRequest.getStudentNumber(), clubNonMember.getStudentNumber()) ||
                !Objects.equals(clubNonMemberProfileRequest.getMajor(), clubNonMember.getMajor()) ||
                !Objects.equals(clubNonMemberProfileRequest.getUserHp(), clubNonMember.getUserHp())) {
            throw new ClubMemberException(ExceptionType.CLUB_MEMBER_NOT_EXISTS);
        }
        return clubNonMember;
    }

    // 두 프로필 값이 같은지 비교
    private void compareProfile(ClubMemberProfileRequest clubMemberTempRequest, ClubMemberProfileRequest clubNonMemberRequest) {
        List<String> message = new ArrayList<>();

        if (!clubMemberTempRequest.getUserName().equals(clubNonMemberRequest.getUserName())) {
            message.add("이름");
        }
        if (!clubMemberTempRequest.getStudentNumber().equals(clubNonMemberRequest.getStudentNumber())) {
            message.add("학번");
        }
        if (!clubMemberTempRequest.getMajor().equals(clubNonMemberRequest.getMajor())) {
            message.add("학과");
        }
        if (!clubMemberTempRequest.getUserHp().equals(clubNonMemberRequest.getUserHp())) {
            message.add("전화번호");
        }

        // 프로필이 일치하지 않는 경우
        if (!message.isEmpty()) {
            throw new ProfileException(ExceptionType.PROFILE_VALUE_MISMATCH, message);
        }
    }
}
