package com.USWCicrcleLink.server.user.service;

import com.USWCicrcleLink.server.aplict.domain.Aplict;
import com.USWCicrcleLink.server.aplict.domain.AplictStatus;
import com.USWCicrcleLink.server.aplict.repository.AplictRepository;
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
import com.USWCicrcleLink.server.user.dto.MyAplictResponse;
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
    private final AplictRepository aplictRepository;
    private final S3FileUploadService s3FileUploadService;
    private final ClubMainPhotoRepository clubMainPhotoRepository;
    private final FloorPhotoRepository floorPhotoRepository;

    //어세스토큰에서 유저정보 가져오기
    private User getUserByAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.user();
    }

    //클럽멤버를 통해 클럽아이디 조회
    private List<MyClubResponse> getMyClubs(List<ClubMembers> clubMembers) {
        return clubMembers.stream()
                .map(ClubMembers::getClub)
                .map(this::myClubResponse)
                .collect(Collectors.toList());
    }

    //프로필을 통해 클럽 멤버 조회
    private List<ClubMembers>getClubMembersByProfileId(Long profileId){
        List<ClubMembers> clubMembers = clubMembersRepository.findByProfileProfileId(profileId);
        if (clubMembers.isEmpty()) {
            return List.of();}
        return clubMembers;
    }

    //소속된 동아리 조회
    public List<MyClubResponse> getMyClubById(){
        User user = getUserByAuth();
        Profile profile = getProfileByUserId((user.getUserId()));
        List<ClubMembers> clubMembers = getClubMembersByProfileId(profile.getProfileId());
        log.info("소속 동아리 조회 완료 {}", user.getUserId());
        return getMyClubs(clubMembers);
    }

    // 지원한 동아리 조회
    public List<MyAplictResponse> getAplictClubById() {
        User user = getUserByAuth();
        Profile profile = getProfileByUserId(user.getUserId());

        // Profile ID를 기반으로 지원 내역 조회
        List<Aplict> aplicts = getAplictsByProfileId(profile.getProfileId());
        log.info("지원 동아리 조회 완료 - User ID: {}", user.getUserId());

        return aplicts.stream()
                .map(aplict -> {
                    Club club = getClubByAplictId(aplict.getAplictId());  // ID 기반 조회로 변경
                    AplictStatus aplictStatus = aplict.getAplictStatus();
                    return myAplictResponse(club, aplictStatus);
                })
                .collect(Collectors.toList());
    }

    //유저아이디를 통해 프로필아이디 조회
    private Profile getProfileByUserId(Long userId) {
        return profileRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ProfileException(ExceptionType.PROFILE_NOT_EXISTS));
    }

    //프로필아이디를 통해 어플릭트 아이디 조회
    private List<Aplict> getAplictsByProfileId(Long profileId) {
        List<Aplict> aplicts = aplictRepository.findByProfileProfileId(profileId);
        if (aplicts.isEmpty()) {
            return List.of();}
        return aplicts;
    }

    // 어플리케이션 ID를 통해 클럽 조회
    private Club getClubByAplictId(Long aplictId) {
        return aplictRepository.findClubByAplictId(aplictId)
                .orElseThrow(() -> new ClubException(ExceptionType.CLUB_NOT_EXISTS));
    }

    //사진 조회 url
    private String getClubMainPhotoUrl(Club club) {
        return Optional.ofNullable(clubMainPhotoRepository.findByClub_ClubId(club.getClubId()))
                .map(clubMainPhoto -> s3FileUploadService.generatePresignedGetUrl(clubMainPhoto.getClubMainPhotoS3Key()))
                .orElse(null);
    }

    //동아리 정보 + 지원현황 가져오기
    private MyAplictResponse myAplictResponse(Club club, AplictStatus aplictStatus){

        String mainPhotoUrl = getClubMainPhotoUrl(club);

        MyAplictResponse myAplictResponse = new MyAplictResponse(
                club.getClubUUID(),
                mainPhotoUrl,
                club.getClubName(),
                club.getLeaderName(),
                club.getLeaderHp(),
                club.getClubInsta(),
                club.getClubRoomNumber(),
                aplictStatus
        );
        return myAplictResponse;
    }
    //동아리 정보 가져오기
    private MyClubResponse myClubResponse(Club club){

        String mainPhotoUrl = getClubMainPhotoUrl(club);

        MyClubResponse myClubResponse = new MyClubResponse(
                club.getClubUUID(),
                mainPhotoUrl,
                club.getClubName(),
                club.getLeaderName(),
                club.getLeaderHp(),
                club.getClubInsta(),
                club.getClubRoomNumber()
        );
        return  myClubResponse;
    }

    //동아리 방 사진 조회
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

        return new ClubFloorPhotoResponse(floorPhoto.getFloor(),presignedUrl);
    }

}
