package com.USWCicrcleLink.server.clubLeader.service;

import com.USWCicrcleLink.server.clubApplication.domain.ClubApplication;
import com.USWCicrcleLink.server.clubApplication.domain.ClubApplicationStatus;
import com.USWCicrcleLink.server.clubLeader.dto.FcmMessageDto;
import com.USWCicrcleLink.server.clubLeader.dto.FcmTokenRequest;
import com.USWCicrcleLink.server.global.security.details.CustomUserDetails;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import com.USWCicrcleLink.server.user.domain.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class FcmServiceImpl implements FcmService {

    private final ProfileRepository profileRepository;

    @Value("${firebase.config-path}")
    private String fireBaseConfigPath;
    private final String FCM_API_URL = "https://fcm.googleapis.com/v1/projects/usw-circle-link/messages:send";
    private final String GOOGLE_AUTH_URL = "https://www.googleapis.com/auth/cloud-platform";
    private final String CLUB_APPLICATION_TITLE_MESSAGE = "동아리 지원 결과";
    private final String CLUB_APPLICATION_PASS_MESSAGE = "에 합격했습니다.";
    private final String CLUB_APPLICATION_FAIL_MESSAGE = "에 불합격했습니다.";
    private final String CLUB_APPLICATION_ERROR_MESSAGE = "관리자에게 문의 해주세요.";

    private static final int FCM_TOKEN_CERTIFICATION_TIME = 60;

    // 메시지 구성, 토큰 받고 메시지 처리
    @Override
    public int sendMessageTo(ClubApplication clubApplication, ClubApplicationStatus clubApplicationResult) throws IOException {
        String fcmToken = clubApplication.getProfile().getFcmToken();
        if (fcmToken == null || fcmToken.trim().isEmpty()) {
            log.warn("FCM 토큰이 없음. 프로필: {} 알림 전송 생략", clubApplication.getProfile().getProfileId());
            return 0;
        }

        try {
            // 메시지 구성
            String message = makeMessage(clubApplication, clubApplicationResult);

            RestTemplate restTemplate = new RestTemplate();
            // 한글 설정
            restTemplate.getMessageConverters()
                    .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", " Bearer " + getAccessToken());

            HttpEntity entity = new HttpEntity<>(message, headers);
            ResponseEntity response = restTemplate.exchange(FCM_API_URL, HttpMethod.POST, entity, String.class);

            // 푸시 알림 전송 실패 예외처리
            if (response.getStatusCode() == HttpStatus.OK) {
                log.debug("푸시 알림 전송 완료: UUID: {}", clubApplication.getClubApplicationUUID());
                return 1;
            } else {
                // FCM 토큰아 유효하지 않음
                if (response.getStatusCode() == HttpStatus.UNAUTHORIZED || response.getStatusCode() == HttpStatus.BAD_REQUEST) {
                    // FCM 토큰 삭제 처리
                    profileRepository.findById(clubApplication.getProfile().getProfileId())
                            .ifPresent(profile -> {
                                profile.updateFcmToken(null); // 토큰 무효화
                                profileRepository.save(profile);
                            });
                    log.warn("FCM 토큰이 유효하지 않음. 프로필: {} FCM 토큰 삭제", clubApplication.getProfile().getProfileId());
                }
                return 0;
            }
        } catch (IOException e) {
            log.error("푸시 알림 전송 실패", e);
            return 0;
        }
    }

    // Firebase Admin SDK의 비공개 키 참조해 bearer 토큰 발급
    private String getAccessToken() throws IOException {
        GoogleCredentials googleCredentials = GoogleCredentials
                .fromStream(new FileInputStream(fireBaseConfigPath))
                .createScoped(List.of(GOOGLE_AUTH_URL));

        // 토큰 만료 ? 갱신 : 토큰
        googleCredentials.refreshIfExpired();
        return googleCredentials.getAccessToken().getTokenValue();
    }

    // FCM 전송 정보를 기반으로 메시지 구성
    // result값에 따라 합불 처리
    private String makeMessage(ClubApplication clubApplication, ClubApplicationStatus clubApplicationResult) throws JsonProcessingException {
        // json변환
        ObjectMapper om = new ObjectMapper();

        // 메시지 제목
        String titleMessage = CLUB_APPLICATION_TITLE_MESSAGE;

        // 메시지 내용
        String bodyMessage;
        if (clubApplicationResult == ClubApplicationStatus.PASS) bodyMessage = clubApplication.getClub().getClubName() + CLUB_APPLICATION_PASS_MESSAGE;
        else if (clubApplicationResult == ClubApplicationStatus.FAIL) bodyMessage = clubApplication.getClub().getClubName() + CLUB_APPLICATION_FAIL_MESSAGE;
        else bodyMessage = CLUB_APPLICATION_ERROR_MESSAGE;

        // 메시지 구성
        FcmMessageDto createFcmMessage = FcmMessageDto.builder()
                .message(FcmMessageDto.Message.builder()
                        .token(clubApplication.getProfile().getFcmToken().trim())
                        .notification(
                                FcmMessageDto.Notification.builder()
                                        .title(titleMessage)
                                        .body(bodyMessage)
                                        .build()
                        )
                        .build())
                .build();

        return om.writeValueAsString(createFcmMessage);
    }

    // fcm token 갱신
    @Transactional
    public void refreshFcmToken(FcmTokenRequest fcmTokenRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userDetails.user();
        log.debug("인증된 사용자: {}", user.getUserAccount());

        // fcm 토큰 갱신
        Optional<Profile> userFcmTokenOptional = profileRepository.findByUserUserId(user.getUserId());
        if (userFcmTokenOptional.isPresent()) {
            Profile userFcmToken = userFcmTokenOptional.get();
            userFcmToken.updateFcmTokenTime(fcmTokenRequest.getFcmToken(), LocalDateTime.now().plusDays(FCM_TOKEN_CERTIFICATION_TIME));
            profileRepository.save(userFcmToken);
            log.debug("사용자 {}의 FCM 토큰 갱신 완료", user.getUserAccount());
        } else {
            log.warn("사용자 {}의 프로필을 찾을 수 없음. FCM 토큰 갱신 실패", user.getUserAccount());
        }
    }
}

