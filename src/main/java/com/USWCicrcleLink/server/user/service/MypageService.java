package com.USWCicrcleLink.server.user.service;

import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import com.USWCicrcleLink.server.clubApplication.repository.ClubApplicationRepository;
import com.USWCicrcleLink.server.club.club.domain.Club;
import com.USWCicrcleLink.server.club.club.domain.ClubMembers;
import com.USWCicrcleLink.server.club.club.domain.FloorPhoto;
import com.USWCicrcleLink.server.club.club.domain.FloorPhotoEnum;
import com.USWCicrcleLink.server.club.club.repository.ClubMainPhotoRepository;
import com.USWCicrcleLink.server.club.club.repository.ClubMembersRepository;
import com.USWCicrcleLink.server.club.club.repository.FloorPhotoRepository;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.BaseException;
import com.USWCicrcleLink.server.global.exception.errortype.ClubException;
import com.USWCicrcleLink.server.global.exception.errortype.ProfileException;
import com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService;
import com.USWCicrcleLink.server.global.security.details.CustomUserDetails;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import com.USWCicrcleLink.server.user.domain.User;
import com.USWCicrcleLink.server.user.dto.ClubFloorPhotoResponse;
import com.USWCicrcleLink.server.user.dto.MyClubApplicationResponse;
import com.USWCicrcleLink.server.user.dto.MyClubResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MypageService {
    private final ClubMembersRepository clubMembersRepository;
    private final ProfileRepository profileRepository;
    private final ClubApplicationRepository clubApplicationRepository;
    private final S3FileUploadService s3FileUploadService;
    private final ClubMainPhotoRepository clubMainPhotoRepository;
    private final FloorPhotoRepository floorPhotoRepository;

    private User getUserByAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.user();
    }

    private List<MyClubResponse> getMyClubs(List<ClubMembers> clubMembers) {
        return clubMembers.stream()
                .map(ClubMembers::getClub)
                .map(this::myClubResponse)
                .collect(Collectors.toList());
    }

    private List<ClubMembers> getClubMembersByProfileId(Long profileId) {
        List<ClubMembers> clubMembers = clubMembersRepository.findByProfileProfileId(profileId);
        if (clubMembers.isEmpty()) {
            return List.of();
        }
        return clubMembers;
    }

    public List<MyClubResponse> getMyClubById() {
        User user = getUserByAuth();
        Profile profile = getProfileByUserId(user.getUserId());
        List<ClubMembers> clubMembers = getClubMembersByProfileId(profile.getProfileId());
        log.info("소속 동아리 조회 완료 {}", user.getUserId());
        return getMyClubs(clubMembers);
    }

    public List<MyClubApplicationResponse> getClubApplications() {
        User user = getUserByAuth();
        Profile profile = getProfileByUserId(user.getUserId());
        List<ClubApplication> clubApplications = getClubApplicationsByProfileId(profile.getProfileId());
        log.info("지원 동아리 조회 완료 - User ID: {}", user.getUserId());

        return clubApplications.stream()
                .map(clubApplication -> {
                    Club club = getClubByClubApplicationId(clubApplication.getClubApplicationId());
                    ClubApplicationStatus clubApplicationStatus = clubApplication.getClubApplicationStatus();
                    return toMyClubApplicationResponse(club, clubApplicationStatus);
                })
                .collect(Collectors.toList());
    }

    private Profile getProfileByUserId(Long userId) {
        return profileRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ProfileException(ExceptionType.PROFILE_NOT_EXISTS));
    }

    private List<ClubApplication> getClubApplicationsByProfileId(Long profileId) {
        List<ClubApplication> clubApplications = clubApplicationRepository.findByProfileProfileId(profileId);
        if (clubApplications.isEmpty()) {
            return List.of();
        }
        return clubApplications;
    }

    private Club getClubByClubApplicationId(Long clubApplicationId) {
        return clubApplicationRepository.findClubByClubApplicationId(clubApplicationId)
                .orElseThrow(() -> new ClubException(ExceptionType.CLUB_NOT_EXISTS));
    }

    private String getClubMainPhotoUrl(Club club) {
        return Optional.ofNullable(clubMainPhotoRepository.findByClub_ClubId(club.getClubId()))
                .map(clubMainPhoto -> s3FileUploadService.generatePresignedGetUrl(clubMainPhoto.getClubMainPhotoS3Key()))
                .orElse(null);
    }

    private MyClubApplicationResponse toMyClubApplicationResponse(Club club, ClubApplicationStatus clubApplicationStatus) {
        String mainPhotoUrl = getClubMainPhotoUrl(club);

        return new MyClubApplicationResponse(
                club.getClubUUID(),
                mainPhotoUrl,
                club.getClubName(),
                club.getLeaderName(),
                club.getLeaderHp(),
                club.getClubInsta(),
                club.getClubRoomNumber(),
                clubApplicationStatus
        );
    }

    private MyClubResponse myClubResponse(Club club) {
        String mainPhotoUrl = getClubMainPhotoUrl(club);

        return new MyClubResponse(
                club.getClubUUID(),
                mainPhotoUrl,
                club.getClubName(),
                club.getLeaderName(),
                club.getLeaderHp(),
                club.getClubInsta(),
                club.getClubRoomNumber()
        );
    }

    public ClubFloorPhotoResponse getClubFloorPhoto(String floor) {
        FloorPhotoEnum floorEnum;

        try {
            floorEnum = FloorPhotoEnum.valueOf(floor);
        } catch (IllegalArgumentException e) {
            throw new BaseException(ExceptionType.INVALID_ENUM_VALUE);
        }

        FloorPhoto floorPhoto = floorPhotoRepository.findByFloor(floorEnum)
                .orElseThrow(() -> new BaseException(ExceptionType.PHOTO_NOT_FOUND));

        String presignedUrl = s3FileUploadService.generatePresignedGetUrl(floorPhoto.getFloorPhotoS3Key());

        return new ClubFloorPhotoResponse(floorPhoto.getFloor(), presignedUrl);
    }
}
