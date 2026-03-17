package com.USWCicrcleLink.server.admin.admin.service;

import com.USWCicrcleLink.server.admin.admin.domain.Admin;
import com.USWCicrcleLink.server.admin.admin.dto.AdminClubCreationRequest;
import com.USWCicrcleLink.server.admin.admin.dto.AdminClubListResponse;
import com.USWCicrcleLink.server.admin.admin.dto.AdminClubPageListResponse;
import com.USWCicrcleLink.server.admin.admin.dto.AdminPwRequest;
import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.domain.ClubMainPhoto;
import com.USWCicrcleLink.server.club.domain.RecruitmentStatus;
import com.USWCicrcleLink.server.club.repository.ClubMainPhotoRepository;
import com.USWCicrcleLink.server.club.repository.ClubRepository;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntro;
import com.USWCicrcleLink.server.clubIntro.domain.ClubIntroPhoto;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroPhotoRepository;
import com.USWCicrcleLink.server.clubIntro.repository.ClubIntroRepository;
import com.USWCicrcleLink.server.clubLeader.domain.Leader;
import com.USWCicrcleLink.server.clubLeader.repository.LeaderRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.AdminException;
import com.USWCicrcleLink.server.global.exception.errortype.BaseException;
import com.USWCicrcleLink.server.global.exception.errortype.ClubException;
import com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService;
import com.USWCicrcleLink.server.global.security.details.CustomAdminDetails;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class AdminClubService {

    private static final String EMPTY_VALUE = "";
    private static final int DEFAULT_CLUB_INTRO_PHOTO_COUNT = 5;

    private final LeaderRepository leaderRepository;
    private final ClubRepository clubRepository;
    private final ClubIntroRepository clubIntroRepository;
    private final ClubMainPhotoRepository clubMainPhotoRepository;
    private final ClubIntroPhotoRepository clubIntroPhotoRepository;
    private final PasswordEncoder passwordEncoder;
    private final S3FileUploadService s3FileUploadService;

    @Transactional(readOnly = true)
    public AdminClubPageListResponse getAllClubs(Pageable pageable) {
        Page<AdminClubListResponse> clubs = clubRepository.findAllWithMemberAndLeaderCount(pageable);
        log.debug("동아리 목록 조회 성공 - 총 {}개", clubs.getTotalElements());

        return AdminClubPageListResponse.builder()
                .content(clubs.getContent())
                .totalPages(clubs.getTotalPages())
                .totalElements(clubs.getTotalElements())
                .currentPage(clubs.getNumber())
                .build();
    }

    public void createClub(AdminClubCreationRequest request) {
        Admin admin = getAuthenticatedAdmin();

        if (!request.getLeaderPw().equals(request.getLeaderPwConfirm())) {
            throw new ClubException(ExceptionType.ClUB_LEADER_PASSWORD_NOT_MATCH);
        }

        validateLeaderAccount(request.getLeaderAccount());
        validateClubName(request.getClubName());

        if (!passwordEncoder.matches(request.getAdminPw(), admin.getAdminPw())) {
            throw new AdminException(ExceptionType.ADMIN_PASSWORD_NOT_MATCH);
        }

        Club club = createClubEntity(request);
        log.info("동아리 생성 성공 - Name: {}", club.getClubName());

        createLeaderAccount(request.getLeaderAccount(), request.getLeaderPw(), club);
        createClubDefaultData(club);
    }

    private Club createClubEntity(AdminClubCreationRequest request) {
        Club club = Club.builder()
                .clubName(request.getClubName())
                .department(request.getDepartment())
                .leaderName(EMPTY_VALUE)
                .leaderHp(EMPTY_VALUE)
                .clubInsta(EMPTY_VALUE)
                .clubRoomNumber(request.getClubRoomNumber())
                .build();

        return clubRepository.save(club);
    }

    private void createLeaderAccount(String leaderAccount, String leaderPw, Club club) {
        Leader leader = Leader.builder()
                .leaderAccount(leaderAccount)
                .leaderPw(passwordEncoder.encode(leaderPw))
                .leaderUUID(UUID.randomUUID())
                .role(Role.LEADER)
                .club(club)
                .build();
        leaderRepository.save(leader);
        log.info("회장 계정 생성 성공 - uuid: {}", leader.getLeaderUUID());
    }

    private void createClubDefaultData(Club club) {
        createClubMainPhoto(club);
        ClubIntro clubIntro = createClubIntro(club);
        createClubIntroPhotos(clubIntro);
    }

    private void createClubMainPhoto(Club club) {
        clubMainPhotoRepository.save(
                ClubMainPhoto.builder()
                        .club(club)
                        .clubMainPhotoName(EMPTY_VALUE)
                        .clubMainPhotoS3Key(EMPTY_VALUE)
                        .build()
        );
    }

    private ClubIntro createClubIntro(Club club) {
        return clubIntroRepository.save(
                ClubIntro.builder()
                        .club(club)
                        .clubIntro(EMPTY_VALUE)
                        .googleFormUrl(EMPTY_VALUE)
                        .recruitmentStatus(RecruitmentStatus.CLOSE)
                        .build()
        );
    }

    private void createClubIntroPhotos(ClubIntro clubIntro) {
        List<ClubIntroPhoto> introPhotos = new ArrayList<>();

        for (int i = 1; i <= DEFAULT_CLUB_INTRO_PHOTO_COUNT; i++) {
            introPhotos.add(ClubIntroPhoto.builder()
                    .clubIntro(clubIntro)
                    .clubIntroPhotoName(EMPTY_VALUE)
                    .clubIntroPhotoS3Key(EMPTY_VALUE)
                    .order(i)
                    .build());
        }

        clubIntroPhotoRepository.saveAll(introPhotos);
        log.debug("기본 동아리 소개 사진 {}개 생성 완료 - Club ID: {}", DEFAULT_CLUB_INTRO_PHOTO_COUNT, clubIntro.getClub().getClubId());
    }

    public void validateLeaderAccount(String leaderAccount) {
        if (leaderRepository.existsByLeaderAccount(leaderAccount)) {
            log.warn("동아리 회장 계정 중복 - LeaderAccount: {}", leaderAccount);
            throw new ClubException(ExceptionType.LEADER_ACCOUNT_ALREADY_EXISTS);
        }
    }

    public void validateClubName(String clubName) {
        if (clubRepository.existsByClubName(clubName)) {
            log.warn("동아리명 중복 - ClubName: {}", clubName);
            throw new ClubException(ExceptionType.CLUB_NAME_ALREADY_EXISTS);
        }
    }

    @Transactional
    public void deleteClub(UUID clubUUID, AdminPwRequest request) {
        Admin admin = getAuthenticatedAdmin();

        if (!passwordEncoder.matches(request.getAdminPw(), admin.getAdminPw())) {
            throw new AdminException(ExceptionType.ADMIN_PASSWORD_NOT_MATCH);
        }

        Long clubId = clubRepository.findClubIdByClubUUID(clubUUID)
                .orElseThrow(() -> {
                    log.warn("동아리 삭제 실패 - 존재하지 않는 Club UUID: {}", clubUUID);
                    return new ClubException(ExceptionType.CLUB_NOT_EXISTS);
                });

        List<String> s3FileKeys = getDeletableS3FileKeys(clubId);

        try {
            clubRepository.deleteClubAndDependencies(clubId);
            registerS3DeletionAfterCommit(s3FileKeys);
            log.info("동아리 삭제 성공 - Club uuid: {}", clubUUID);
        } catch (Exception e) {
            log.error("동아리 삭제 중 오류 발생 - Club uuid: {}, 오류: {}", clubUUID, e.getMessage());
            throw new BaseException(ExceptionType.SERVER_ERROR, e);
        }
    }

    private List<String> getDeletableS3FileKeys(Long clubId) {
        List<String> s3FileKeys = new ArrayList<>();

        clubMainPhotoRepository.findS3KeyByClubId(clubId)
                .filter(this::hasText)
                .ifPresent(s3FileKeys::add);

        clubIntroPhotoRepository.findByClubIntroClubId(clubId).stream()
                .map(ClubIntroPhoto::getClubIntroPhotoS3Key)
                .filter(this::hasText)
                .forEach(s3FileKeys::add);

        return s3FileKeys;
    }

    private void registerS3DeletionAfterCommit(List<String> s3FileKeys) {
        if (s3FileKeys.isEmpty()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                s3FileUploadService.deleteFiles(s3FileKeys);
            }
        });
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Admin getAuthenticatedAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomAdminDetails adminDetails = (CustomAdminDetails) authentication.getPrincipal();
        return adminDetails.admin();
    }
}
