package com.USWCicrcleLink.server.user.service;

import com.USWCicrcleLink.server.club.domain.Club;
import com.USWCicrcleLink.server.club.repository.ClubRepository;
import com.USWCicrcleLink.server.email.domain.EmailToken;
import com.USWCicrcleLink.server.email.repository.EmailTokenRepository;
import com.USWCicrcleLink.server.email.service.EmailService;
import com.USWCicrcleLink.server.email.service.EmailTokenService;
import com.USWCicrcleLink.server.global.bucket4j.RateLimite;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.errortype.*;
import com.USWCicrcleLink.server.global.security.Integration.service.IntegrationAuthService;
import com.USWCicrcleLink.server.global.security.details.CustomUserDetails;
import com.USWCicrcleLink.server.global.security.jwt.JwtProvider;
import com.USWCicrcleLink.server.global.security.jwt.dto.TokenDto;
import com.USWCicrcleLink.server.profile.domain.Profile;
import com.USWCicrcleLink.server.profile.repository.ProfileRepository;
import com.USWCicrcleLink.server.profile.service.ProfileService;
import com.USWCicrcleLink.server.user.domain.AuthToken;
import com.USWCicrcleLink.server.user.domain.ExistingMember.ClubMemberTemp;
import com.USWCicrcleLink.server.user.domain.User;
import com.USWCicrcleLink.server.user.domain.WithdrawalToken;
import com.USWCicrcleLink.server.user.dto.*;
import com.USWCicrcleLink.server.user.repository.ClubMemberTempRepository;
import com.USWCicrcleLink.server.user.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;
    private final ProfileService profileService;
    private final ClubMemberTempRepository clubMemberTempRepository;
    private final ClubRepository clubRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final EmailService emailService;
    private final EmailTokenService emailTokenService;
    private final ClubMemberAccountStatusService clubMemberAccountStatusService;
    private final PasswordService passwordService;
    private final IntegrationAuthService integrationAuthService;

    private static final int FCM_TOKEN_CERTIFICATION_TIME = 60;


    // 어세스토큰에서 유저정보 가져오기
    public User getUserByAuth() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userDetails.user();
    }

    //현재 비밀번호 확인
    private boolean confirmPW(String userpw) {
        User user = getUserByAuth();
        return passwordEncoder.matches(userpw, user.getUserPw());
    }

    //비밀번호 변경
    public void updateNewPW(UpdatePwRequest updatePwRequest, HttpServletResponse response) {

        User user = getUserByAuth();

        if (!confirmPW(updatePwRequest.getUserPw())) {
            throw new UserException(ExceptionType.USER_PASSWORD_NOT_MATCH);
        }

        if (passwordEncoder.matches(updatePwRequest.getNewPw(), user.getUserPw())) {
            throw new UserException(ExceptionType.USER_PASSWORD_NOT_REUSE);
        }

        // 비밀번호 칸이 빈칸인지 확인
        passwordService.checkPasswordFieldBlank(updatePwRequest.getNewPw(), updatePwRequest.getNewPw());
        // 새로운 비밀번호의 유효성 검사
        passwordService.checkPasswordCondition(updatePwRequest.getNewPw());
        // 비밀번호가 일치하는지 확인
        passwordService.checkPasswordMatch(updatePwRequest.getNewPw(), updatePwRequest.getConfirmNewPw());

        String encryptedNewPw = passwordEncoder.encode(updatePwRequest.getNewPw());
        user.updateUserPw(encryptedNewPw);

        try {
            userRepository.save(user);  // 비밀번호 업데이트
        } catch (Exception e) {
            log.error("비밀번호 업데이트 실패 {}", user.getUserId());
            throw new UserException(ExceptionType.PROFILE_UPDATE_FAIL);
        }

        jwtProvider.deleteRefreshToken(user.getUserUUID());
        jwtProvider.deleteRefreshTokenCookie(response);
        log.info("비밀번호 변경 완료: {}", user.getUserId());
    }

    // 전화번호 - 제거하는 메서드
    public String removeHyphensFromPhoneNumber(String telephone) {
        if (telephone.contains("-")) {
            return telephone.replaceAll("-", "");
        }
        return telephone;
    }


    // 신규 회원 가입
    public void signUpUser(SignUpRequest request,String email) {
        // user 객체 생성
        User user = createUser(request,email);
        userRepository.save(user);
        log.debug("user 객체 생성: user_uuid={}",user.getUserUUID());

        // profile 객체 생성
        Profile profile = createProfile(user, request);
        profileRepository.save(profile);
        log.debug("profile 객체 생성: profile_id={}",profile.getProfileId());


        // emailToken 테이블 삭제
        emailTokenService.deleteEmailToken(email);
        log.debug("임시 회원 정보 삭제 완료");
    }

    // user 객체 생성
    @Transactional
    public User createUser(SignUpRequest request,String email) {
        // 비밀번호 인코딩
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        // user 객체 생성하기
        try {
            return User.createUser(request, encodedPassword,email);
        } catch (Exception e) {
            log.error("이메일 인증 후, 회원가입 진행 중 user객체를 생성하는 과정에서 오류 발생");
            throw new UserException(ExceptionType.USER_CREATION_FAILED);
        }
    }

    // profile 객체 생성
    @Transactional
    public Profile createProfile(User user, SignUpRequest request) {

        // 번호만 추출해서 저장하기( - 입력 방지)
        String telephone = removeHyphensFromPhoneNumber(request.getTelephone());
        try {
            return Profile.createProfile(user, request, telephone);
        } catch (Exception e) {
            log.error("이메일 인증 후, 회원가입 진행 중 profile 객체를 생성하는 과정에서 오류 발생");
            throw new ProfileException(ExceptionType.PROFILE_CREATION_FAILED);
        }
    }

    // 기존 회원가입 - 임시 동아리원 생성하기
    public ClubMemberTemp registerClubMemberTemp(ExistingMemberSignUpRequest request) {
        log.debug("임시 동아리원 등록 시작 - 이름: {}, 전화번호: {}, 지원한 동아리 개수: {}",
                request.getUserName(), request.getTelephone(), request.getClubs().size());

        // 지원한 동아리의 개수
        int total = request.getClubs().size();

        // 전화번호 - 제거
        String telephone = removeHyphensFromPhoneNumber(request.getTelephone());
        log.debug("전화번호 형식 변환 완료 - 원본: {}, 변환 후: {}", request.getTelephone(), telephone);

        // 비밀번호 인코딩
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        log.debug("비밀번호 인코딩 완료 - 사용자 이름: {}", request.getUserName());

        // 엔터티 저장
        ClubMemberTemp savedEntity;
        try {
            savedEntity = clubMemberTempRepository.save(request.toEntity(encodedPassword, telephone, total));
            log.debug("임시 동아리원 등록 완료 - 저장된 엔터티: {}", savedEntity);
        } catch (Exception e) {
            log.error("임시 동아리원 등록 실패 - 사용자 이름: {}", request.getUserName());
            throw new ClubMemberTempException(ExceptionType.CLUB_MEMBERTEMP_CREATE_FAILED);
        }

        return savedEntity;
    }


    // 동아리 회장에게 가입신청 보내기
    public void sendRequest(ExistingMemberSignUpRequest request, ClubMemberTemp clubMemberTemp) {
        log.debug("가입신청 시작 - 사용자: {}, 요청 동아리 개수: {}",
                clubMemberTemp.getProfileTempName(), clubMemberTemp.getTotalClubRequest());

        // 동아리 정보 조회
        for (ClubDTO clubDto : request.getClubs()) {
            log.debug("동아리 정보 조회 중 - Club UUID: {}", clubDto.getClubUUID());
            Club club = clubRepository.findByClubUUID(clubDto.getClubUUID())
                    .orElseThrow(() -> {
                        log.error("존재하지않는 동아리 UUID:{}", clubDto.getClubUUID());
                        return new ClubException(ExceptionType.CLUB_NOT_EXISTS);
                    });
            log.debug("동아리 조회 성공 - Club ID: {}, 동아리 이름: {}", club.getClubId(), club.getClubName());

            // accountStatus 객체 생성하기
            clubMemberAccountStatusService.createAccountStatus(club, clubMemberTemp);
        }
        // 요청이 전부 제대로 갔는지 검증
        clubMemberAccountStatusService.checkRequest(request, clubMemberTemp);
    }

    // 이메일 중복 검증
    public void verifyUserDuplicate(String email) {
        log.debug("이메일 중복 검증 시작 email= {}", email);
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    throw new UserException(ExceptionType.USER_OVERLAP);
                });
        log.debug("이메일 중복 검증 완료");
    }

    // 이메일 토큰의 유효성 확인
    public EmailToken verifyEmailToken(UUID emailTokenUUID) {

        // 이메일 토큰 검증
        EmailToken emailToken = emailTokenService.verifyEmailToken(emailTokenUUID);

        // 이메일 토큰 인증 완료 처리
        try {
            emailToken.verifyEmail();
            emailTokenRepository.save(emailToken);
        } catch (Exception e) {
            log.error("이메일 토큰 인증 완료처리 중 오류가 발생함");
            throw new EmailException(ExceptionType.EMAIL_TOKEN_STATUS_UPATE_FALIED);
        }
        return emailToken;
    }

    // 아이디 중복 확인
    public void verifyAccountDuplicate(String account) {
        log.debug("계정 중복 체크 요청 시작 account = {}", account);
        if (userRepository.findByUserAccount(account).isPresent() || clubMemberTempRepository.findByProfileTempAccount(account).isPresent()) {
            throw new UserException(ExceptionType.USER_ACCOUNT_OVERLAP);
        }
        log.debug("계정 중복 체크 완료. account = {}", account);
    }

    @Transactional(readOnly = true)
    public User findUser(String email) {
        log.debug("계정 찾기 요청  email= {}", email);
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(ExceptionType.USER_NOT_EXISTS));
    }

    @Transactional(readOnly = true)
    public User validateAccountAndEmail(UserInfoDto request) {
        log.debug("아이디와 이메일 유효성 검증 시작");
        return userRepository.findByUserAccountAndEmail(request.getUserAccount(), request.getEmail())
                .orElseThrow(() -> new UserException(ExceptionType.USER_INVALID_ACCOUNT_AND_EMAIL));
    }

    // 비밀번호 재설정
    public void resetPW(UUID uuid, PasswordRequest request) {

        // 회원 조회
        User user = userRepository.findByUserUUID(uuid).orElseThrow(() -> new UserException(ExceptionType.USER_UUID_NOT_FOUND));

        // 이전 비밀번호와 새로 설정할 비밀번호가 일치하는 경우
        if(passwordEncoder.matches(request.getPassword(), user.getUserPw())){
            log.debug("이전 비밀번호와 새롭게 설정한 비밀번호가 일치함");
            throw new UserException(ExceptionType.USER_PASSWORD_NOT_REUSE);
        }

        // 새로운 비밀번호의 유효성 검사
        passwordService.validatePassword(request.getPassword(), request.getConfirmPassword());

        log.debug("비밀번호 유효성 검증 완료");

        user.updateUserPw(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);
        jwtProvider.deleteRefreshToken(user.getUserUUID());

        log.debug("새로운 비밀번호 변경 완료 userUUID = {}", user.getUserUUID());
    }

    // 회원 가입 메일 생성 및 전송
    @RateLimite(action = "EMAIL_VERIFICATION")
    public void sendSignUpMail(EmailToken emailToken) {
        log.debug("회원 가입 인증 메일 요청 ");
        MimeMessage message = emailService.createSignUpLink(emailToken);
        emailService.sendEmail(message);
        log.debug("회원가입 인증메일 전송 완료 emailToken_uuid= {} ", emailToken.getEmailTokenUUID());
    }

    // 비밀번호 변경을 위한 인증 코드 메일 전송
    public void sendAuthCodeMail(User user, AuthToken authToken) {
        log.debug("비밀번호 찾기  메일 생성 요청");
        MimeMessage message = emailService.createAuthCodeMail(user, authToken);
        emailService.sendEmail(message);
        log.debug("비밀번호 찾기 메일 전송 완료");
    }

    // 아이디 찾기 메일 전송
    @RateLimite(action = "ID_FOUND_EMAIL")
    public void sendAccountInfoMail(User findUser) {
        log.debug("아이디 찾기 메일 생성 요청");
        MimeMessage message = emailService.createAccountInfoMail(findUser);
        emailService.sendEmail(message);
        log.debug("아이디 찾기 메일 전송 완료 email=  {} ", findUser.getEmail());
    }

    // 회원 탈퇴 메일 전송
    public void sendWithdrawalCodeMail(WithdrawalToken token) {
        log.debug("회원 탈퇴 메일 생성 요청");
        User findUser = getUserByAuth();
        MimeMessage message = emailService.createWithdrawalCodeMail(findUser, token);
        emailService.sendEmail(message);
        log.debug("회원 탈퇴 메일 전송 완료 email=  {} ", findUser.getEmail());
    }
    /**
     * User 로그인
     */
    public TokenDto userLogin(LogInRequest request, HttpServletResponse response) {

        User user = userRepository.findByUserAccount(request.getAccount()).orElse(null);

        // 유저 객체가 존재하는지 확인
        if (user == null) {
            // 기존 회원 가입 요청을 보낸 사람인지 확인(비회원 확인)
            Optional<ClubMemberTemp> clubMemberTemp = clubMemberTempRepository.findByProfileTempAccount(request.getAccount());
            if (clubMemberTemp.isPresent() && passwordEncoder.matches(request.getPassword(), clubMemberTemp.get().getProfileTempPw())) {
                throw new UserException(ExceptionType.USER_NONMEMBER);
            } else { // 제3자의 요청인 경우
                throw new UserException(ExceptionType.THIRD_PARTY_LOGIN_ATTEMPT);
            }
        }

        // 아이디 비밀번호 일치 불일치 여부 확인
        if (!passwordEncoder.matches(request.getPassword(), user.getUserPw())) {
            throw new UserException(ExceptionType.USER_AUTHENTICATION_FAILED);
        }

        UUID userUUID = user.getUserUUID();
        Profile profile = profileRepository.findByUser_UserUUID(userUUID)
                .orElseThrow(() -> new ProfileException(ExceptionType.PROFILE_NOT_EXISTS));

        log.debug("프로필 조회 성공 - 사용자 UUID: {}, 회원 타입: {}", userUUID, profile.getMemberType());


        String accessToken = jwtProvider.createAccessToken(userUUID, user.getRole(), response);
        String refreshToken = jwtProvider.createRefreshToken(userUUID, user.getRole(), response);

        // FCM 토큰 업데이트
        if (request.getFcmToken() != null && !request.getFcmToken().isEmpty()) {
            profile.updateFcmTokenTime(request.getFcmToken(), LocalDateTime.now().plusDays(FCM_TOKEN_CERTIFICATION_TIME));
            profileRepository.save(profile);
            log.debug("FCM 토큰 업데이트 완료: {}", user.getUserAccount());
        }

        log.debug("로그인 성공, UUID: {}", userUUID);
        return new TokenDto(accessToken, refreshToken);
    }

    public EmailToken checkEmailDuplication(String email) {
        log.debug("이메일 중복 확인 시작, email: {}", email);

        EmailToken emailToken;

        // 실제 사용중인 이메일 확인
        if (userRepository.findByEmail(email).isPresent()) {
            log.error("user 테이블에서 중복된 이메일 존재, email 값= {}", email);
            throw new UserException(ExceptionType.USER_OVERLAP);
        }
        else if (emailTokenRepository.findByEmail(email).isPresent()) {
            log.debug("요청 가입 대기자 존재 - emailToken 테이블에서 중복된 이메일 존재, email 값= {}", email);

            // 토큰 만료시간 업데이트
            emailToken = emailTokenService.getEmailTokenByEmail(email);
            log.debug("emailToken 조회 완료, emailTokenUUID= {}", emailToken.getEmailTokenUUID());
            emailTokenService.updateCertificationTime(emailToken);
            log.debug("이메일 인증 만료시간 업데이트 완료, emailTokenUUID= {}", emailToken.getEmailTokenUUID());
        } else{
            // 그 외의 경우 새로운 이메일 토큰 생성
            emailToken = emailTokenService.createEmailToken(email);
        }
        log.debug("이메일 중복 확인 완료");

        return emailToken;
    }


    // 이메일 인증 받은 사용자인지 검증하기
    public String isEmailVerified(UUID emailTokenUUID,UUID requestSignupUUID) {

        // 이메일 토큰 조회
        EmailToken emailToken = emailTokenService.getEmailTokenByEmailTokenUUID(emailTokenUUID);

        // 이메일 토큰 조회로 찾은 signupuuid와 프론트에서 가지고 있던 signupuuid비교
        if(!emailToken.getSignupUUID().equals(requestSignupUUID)){
            throw new UserException(ExceptionType.USER_UUID_IS_NOT_VALID);
        }

        return emailToken.getEmail();
    }

    /**
     * 회원 탈퇴 (User)
     */
    public void cancelMembership(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtProvider.resolveRefreshToken(request);

        if (refreshToken == null) {
            integrationAuthService.logout(request, response);
            return;
        }

        try {
            jwtProvider.validateRefreshToken(refreshToken, request);
            UUID userUUID = jwtProvider.getUUIDFromRefreshToken(refreshToken);

            // FCM 토큰 삭제 (모바일 푸시 알림 무효화)
            profileRepository.findByUser_UserUUID(userUUID).ifPresent(profile -> {
                profile.updateFcmToken(null);
                profileRepository.save(profile);
                log.debug("회원 탈퇴 - FCM 토큰 삭제 완료 - UUID: {}", userUUID);
            });

            // 회원 정보 삭제
            profileService.deleteProfileByUserUUID(userUUID);
            userRepository.deleteByUserUUID(userUUID);

            log.info("회원 탈퇴 성공 - UUID: {}", userUUID);
        } catch (TokenException ignored) {
        } finally {
            integrationAuthService.logout(request, response);
        }
    }

    // 신규회원가입 전, 조건 검사
    public void checkNewSignupCondition(SignUpRequest request){
        log.debug("신규 회원가입 요청 처리전, 3가지 조건 검사");

        log.debug("1- 아이디 중복 확인 검사");
        verifyAccountDuplicate(request.getAccount());

        log.debug("2- 비밀번호 유효성 검사");
        passwordService.validatePassword(request.getPassword(), request.getConfirmPassword());

        log.debug("3- 프로필 중복 확인 검사");
        profileService.checkProfileDuplicated(request.getUserName(),request.getStudentNumber(), request.getTelephone());

    }

    // 기존 회원 가입 전 조건 검사
    //fixme 기존회원가입 시 검사해야하는 조건 생각해보기(프로필 중복확인)
    public void checkExistingSignupCondition(ExistingMemberSignUpRequest request) {

        // 아이디 중복 확인 검사
        verifyAccountDuplicate(request.getAccount());

        // 비밀번호 유효성 검사
        passwordService.validatePassword(request.getPassword(),request.getConfirmPassword());

        // clubMemberTemp 테이블에서 프로필 중복 확인(이름&&학번&&전화번호)
        checkClubMemberTempProfileDuplicate(request.getUserName(), request.getStudentNumber(), request.getTelephone());
    }

    // clubMemberTemp에서 이메일로 중복 확인
    public void verifyClubMemberTempDuplicate(String email) {
        clubMemberTempRepository.findByProfileTempEmail(email)
                .ifPresent(clubMemberTemp -> {
                    throw new ClubMemberTempException(ExceptionType.CLUB_MEMBERTEMP_IS_DUPLICATED);
                });
    }


    // clubMemberTemp 테이블에서 프로필 중복 확인(이름&&학번&&전화번호)
    public void checkClubMemberTempProfileDuplicate(String name,String studentNumber,String hp){
        clubMemberTempRepository.findByProfileTempNameAndProfileTempStudentNumberAndAndProfileTempHp(name,studentNumber,hp)
                .ifPresent(clubMemberTemp -> {
                    throw new ClubMemberTempException(ExceptionType.CLUB_MEMBERTEMP_IS_EXISTS);
                });
    }

    // 이메일 중복 확인
    public void verifyEmailDuplicate(String email) {
        // user테이블에서 중복 확인
        verifyUserDuplicate(email);
        // clubMemberTemp에서 이메일로 중복 확인
        verifyClubMemberTempDuplicate(email);
    }

}
