package com.USWCicrcleLink.server.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

@Getter
@AllArgsConstructor
public enum ExceptionType {
//    100-199: 인증 및 권한
//    200-299: 사용자 관리
//    300-399: 데이터 관리
//    400-499: 토큰 및 인증
//    500-599: 메일 및 통신

    /**
     * SERVER ERROR
     */
    // ======= 공통 예외 =======
    SERVER_ERROR("COM-501", "서버 오류입니다. 관리자에게 문의해주세요.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT("COM-302", "잘못된 입력 값입니다.", HttpStatus.BAD_REQUEST),

    /**
     * Domain: EmailToken
     */
    EMAIL_TOKEN_NOT_FOUND("EMAIL_TOKEN-001", "해당 토큰이 존재하지 않습니다.", BAD_REQUEST),
    EMAIL_TOKEN_IS_EXPIRED("EMAIL_TOKEN-002", "토큰이 만료되었습니다. 다시 이메일인증 해주세요", BAD_REQUEST),
    EMAIL_TOKEN_CREATION_FALILED("EMAIL_TOKEN-003", "이메일 토큰 생성중 오류가 발생했습니다.", INTERNAL_SERVER_ERROR),
    EMAIL_TOKEN_STATUS_UPATE_FALIED("EMAIL_TOKEN-004", "이메일 토큰의 필드 업데이트 후, 저장하는 과정에서 오류가 발생했습니다.", INTERNAL_SERVER_ERROR),
    EMAIL_TOKEN_NOT_VERIFIED("EMAIL_TOKEN-005", "인증이 완료되지 않은 이메일 토큰입니다.",BAD_REQUEST),


    /**
     * Domain: User
     */
    USER_NOT_EXISTS("USR-201", "사용자가 존재하지 않습니다.", BAD_REQUEST),
    USER_NEW_PASSWORD_NOT_MATCH("USR-202","두 비밀번호가 일치하지 않습니다.", BAD_REQUEST),
    USER_PASSWORD_NOT_INPUT("USR-203","비밀번호 값이 빈칸입니다",BAD_REQUEST),
    USER_PASSWORD_NOT_MATCH("USR-204","현재 비밀번호와 일치하지 않습니다",BAD_REQUEST),
    USER_PASSWORD_UPDATE_FAIL("USR-205","비밀번호 업데이트에 실패했습니다",INTERNAL_SERVER_ERROR),
    USER_OVERLAP("USR-206", "이미 존재하는 회원입니다.", CONFLICT),
    USER_ACCOUNT_OVERLAP("USR-207", "계정이 중복됩니다.", CONFLICT),
    USER_INVALID_ACCOUNT_AND_EMAIL("USR-209", "올바르지 않은 이메일 혹은 아이디입니다.", BAD_REQUEST),
    USER_UUID_NOT_FOUND("USR-210","회원의 uuid를 찾을 수 없습니다.", BAD_REQUEST),
    USER_AUTHENTICATION_FAILED("USR-211","아이디 혹은 비밀번호가 일치하지 않습니다",UNAUTHORIZED),
    USER_PASSWORD_CONDITION_FAILED("USR-214","영문자,숫자,특수문자는 적어도 1개 이상씩 포함되어야합니다",BAD_REQUEST),
    USER_NONMEMBER("USR-216","비회원 사용자입니다.인증을 완료해주세요",UNAUTHORIZED),
    USER_PASSWORD_NOT_REUSE("USR-217", "현재 비밀번호와 같은 비밀번호로 변경할 수 없습니다.", BAD_REQUEST),
    USER_CREATION_FAILED("USR-218","회원 생성중 오류 발생",INTERNAL_SERVER_ERROR),
    USER_UUID_IS_NOT_VALID("USR-219","요청 받은 SIGNUPUUID가 일치하지 않습니다",UNAUTHORIZED),
    THIRD_PARTY_LOGIN_ATTEMPT("USR-220","제3자의 로그인 요청 시도 입니다",UNAUTHORIZED),


    /**
     * Domain: ClubMemberTemp
     */
    CLUB_MEMBERTEMP_CREATE_FAILED("CMEM-TEMP-301","기존 회원가입 사용자 생성에 실패했습니다",INTERNAL_SERVER_ERROR),
    CLUB_MEMBERTEMP_IS_DUPLICATED("CMEM-TEMP-302","CLUBMEMBERTEMP 테이블에 존재하는 이메일 입니다.",BAD_REQUEST),
    CLUB_MEMBERTEMP_IS_EXISTS("CMEM-TEMP-303","CLUBMEMBERTEMP 에 중복된 프로필이 존재합니다",BAD_REQUEST),
    /**
     * Domain: ClubMemberAccountStatus
     */
    CLUB_MEMBER_ACCOUNTSTATUS_CREATE_FAILED("CMEM-ACST-301","AccountStatus 객체 생성 과정중 오류가 발생했습니다",INTERNAL_SERVER_ERROR),
    CLUB_MEMBER_ACCOUNTSTATUS_SAVE_FAILED("CMEM-ACST-302","AccountStatus 객체 저장 과정중 오류가 발생했습니다",INTERNAL_SERVER_ERROR),
    CLUB_MEMBER_ACCOUNTSTATUS_COUNT_NOT_MATCH("CMEM-ACST-303","사용자가 요청한 개수와 실제 요청된 개수가 다릅니다",INTERNAL_SERVER_ERROR),
    CLUB_MEMBER_ACCOUNTSTATUS_REQEUST_NOT_MATCH("CMEM-ACST-304","사용자가 요청한 동아리와 실제 요청값이 다르게 생성되었습니다",INTERNAL_SERVER_ERROR),


    /**
     * Domain: Security
     */
    INVALID_ROLE("TOK-201", "유효하지 않은 role입니다.", BAD_REQUEST),
    UNAUTHENTICATED_USER("TOK-204", "인증되지 않은 사용자입니다.", UNAUTHORIZED),
    INVALID_TOKEN("TOK-202", "유효하지 않은 토큰입니다.", UNAUTHORIZED),

    /**
     * Domain: Club
     */
    CLUB_NOT_EXISTS("CLUB-201", "존재하지않는 동아리 입니다.", NOT_FOUND),
    ClUB_CHECKING_ERROR("CLUB-202", "동아리 조회 중 오류가 발생했습니다.", INTERNAL_SERVER_ERROR),
    CLUB_NAME_ALREADY_EXISTS("CLUB-203", "이미 존재하는 동아리 이름입니다.", CONFLICT),
    CLUB_MAINPHOTO_NOT_EXISTS("CLUB-204", "동아리 사진이 존재하지 않습니다", NOT_FOUND),
    CLUB_ROOM_ALREADY_EXISTS("CLUB-205", "이미 지정된 동아리방입니다." , CONFLICT),


    /**
     * Domain: ClubCategory
     */
    CATEGORY_NOT_FOUND("CTG-201", "존재하지 않는 카테고리입니다.", NOT_FOUND),
    INVALID_CATEGORY_COUNT("CG-202","카테고리는 최대 3개까지 선택할수 있습니다.", PAYLOAD_TOO_LARGE),
    DUPLICATE_CATEGORY("CTG-203", "이미 존재하는 카테고리입니다.", CONFLICT),

    /**
     * Domain: ClubIntro
     */
    CLUB_INTRO_NOT_EXISTS("CINT-201", "해당 동아리 소개글이 존재하지 않습니다.", NOT_FOUND),
    GOOGLE_FORM_URL_NOT_EXISTS("CINT-202", "구글 폼 URL이 존재하지 않습니다.", BAD_REQUEST),
    INVALID_RECRUITMENT_STATUS("CINT-303", "모집 상태가 올바르지 않습니다.", BAD_REQUEST),

    /**
     * Domain: Admin
     */
    ADMIN_NOT_EXISTS("ADM-201", "해당 계정이 존재하지 않습니다.", NOT_FOUND),
    ADMIN_PASSWORD_NOT_MATCH("ADM-202", "관리자 비밀번호가 일치하지 않습니다.", BAD_REQUEST),

    /**
     * Domain: Notice
     */
    NOTICE_NOT_EXISTS("NOT-201", "공지사항이 존재하지 않습니다.", NOT_FOUND),
    UP_TO_5_PHOTOS_CAN_BE_UPLOADED("NOT-202", "최대 5개의 사진이 업로드 가능합니다.", PAYLOAD_TOO_LARGE),
    NOTICE_PHOTO_NOT_EXISTS("NOT-204", "사진이 존재하지 않습니다.", NOT_FOUND),
    INVALID_PHOTO_ORDER("NOT-205", "사진 순서는 1에서 5 사이여야 합니다.", BAD_REQUEST),
    NOTICE_CHECKING_ERROR("NOT-206", "공지사항 조회 중 에러가 발생했습니다.", BAD_REQUEST),

    /**
     * Domain: Profile
     */
    PROFILE_NOT_EXISTS("PFL-201", "프로필이 존재하지 않습니다.", NOT_FOUND),
    PROFILE_UPDATE_FAIL("PFL-202", "프로필 업데이트에 실패했습니다.", INTERNAL_SERVER_ERROR),
    PROFILE_NOT_INPUT("PFL-203","프로필 입력값은 필수입니다.", BAD_REQUEST),
    DUPLICATE_PROFILE("PFL-204","이미 존재하는 회원입니다.", BAD_REQUEST),
    DEPARTMENT_NOT_INPUT("PFL-205", "학과 정보는 필수 입력 항목입니다.", BAD_REQUEST),
    NOT_NON_MEMBER("PFL-206", "비회원만 수정할 수 있습니다.", BAD_REQUEST),
    PROFILE_ALREADY_EXISTS("PFL-207","프로필이 이미 존재합니다",BAD_REQUEST),
    INVALID_MEMBER_TYPE("PFL-208","유효하지 않은 회원 종류입니다.",BAD_REQUEST),
    PROFILE_VALUE_MISMATCH("PFL-209","프로필 값이 일치하지 않습니다.",BAD_REQUEST),
    PROFILE_CREATION_FAILED("PFL-210","프로필 생성중 오류 발생",INTERNAL_SERVER_ERROR),



    /**
     * Domain: ClubIntroPhoto, Club(MainPhoto)
     */
    PHOTO_ORDER_MISS_MATCH("CLP-201", "범위를 벗어난 사진 순서 값입니다.", BAD_REQUEST),
    CLUB_ID_NOT_EXISTS("CLP-202", "동아리 ID가 존재하지 않습니다.", INTERNAL_SERVER_ERROR),

    /**
     * Domain: ClubLeader
     */
    CLUB_LEADER_ACCESS_DENIED("CLDR-101","동아리 접근 권한이 없습니다.", FORBIDDEN),
    CLUB_LEADER_NOT_EXISTS("CLDR-201","동아리 회장이 존재하지 않습니다.", BAD_REQUEST),
    ClUB_LEADER_PASSWORD_NOT_MATCH("CLDR-202", "동아리 회장 비밀번호가 일치하지 않습니다", BAD_REQUEST),
    LEADER_ACCOUNT_ALREADY_EXISTS("CLDR-203", "이미 존재하는 동아리 회장 계정입니다.", UNPROCESSABLE_ENTITY),
    LEADER_NAME_REQUIRED("CLDR-204", "동아리 회장의 이름은 필수 입력 항목입니다.", BAD_REQUEST),

    /**
     * Domain: ClubMember
     */
    CLUB_MEMBER_NOT_EXISTS("CMEM-201","동아리 회원이 존재하지 않습니다.", NOT_FOUND),
    CLUB_MEMBER_ALREADY_EXISTS("CMEM-202","동아리 회원이 이미 존재합니다.", BAD_REQUEST),

    /**
     * Domain: ClubMemberAccountStatus
     */
    CLUB_MEMBER_SIGN_UP_REQUEST_NOT_EXISTS("CMEMT-201","회원 가입 요청이 존재하지 않습니다.", NOT_FOUND),

    /**
     * Domain: ClubApplication
     */
    CLUB_APPLICATION_NOT_EXISTS("APT-201","지원서가 존재하지 않습니다.", NOT_FOUND),
    APPLICANT_NOT_EXISTS("APT-202","유효한 지원자가 존재하지 않습니다.", NOT_FOUND),
    ADDITIONAL_APPLICANT_NOT_EXISTS("APT-203","유효한 추합 대상자가 존재하지 않습니다.", NOT_FOUND),
    APPLICANT_COUNT_MISMATCH("APT-204", "선택한 지원자 수와 전체 지원자 수가 일치하지 않습니다.", BAD_REQUEST),
    ALREADY_APPLIED("APT-205", "이미 지원한 동아리입니다.", BAD_REQUEST),
    ALREADY_MEMBER("APT-206", "이미 해당 동아리 회원입니다.", BAD_REQUEST),
    PHONE_NUMBER_ALREADY_REGISTERED("APT-207", "이미 등록된 전화번호입니다.", BAD_REQUEST),
    STUDENT_NUMBER_ALREADY_REGISTERED("APT-208", "이미 등록된 학번입니다.", BAD_REQUEST),

    /**
     * Domain: AuthCodeToken
     */
    INVALID_AUTH_CODE("AC-101", "인증번호가 일치하지 않습니다", BAD_REQUEST),
    AUTHCODETOKEN_NOT_EXISTS("AC-102", "인증 코드 토큰이 존재하지 않습니다", BAD_REQUEST),

    /**
     * Domain: WithdrawalToken
     */
    INVALID_WITHDRAWAL_CODE("WT-101", "인증번호가 일치하지 않습니다", BAD_REQUEST),
    WITHDRAWALTOKEN_NOT_EXISTS("WT-102", "탈퇴 토큰이 존재하지 않습니다", BAD_REQUEST),

    /**
     * 공통
     */
    SEND_MAIL_FAILED("EML-501", "메일 전송에 실패했습니다.", INTERNAL_SERVER_ERROR),
    INVALID_UUID_FORMAT("UUID-502", "유효하지 않은 UUID 형식입니다." , BAD_REQUEST),
    TOO_MANY_ATTEMPT("ATTEMPT-503", "최대 시도 횟수를 초과했습니다. 5분 후  다시 시도 하세요", BAD_REQUEST),
    PHOTO_FILE_IS_EMPTY("PHOTO-504","사진 파일이 비어있습니다." ,BAD_REQUEST),
    PHOTO_NOT_FOUND("PHOTO-505", "해당 사진이 존재하지 않습니다.", NOT_FOUND),
    INVALID_ENUM_VALUE("ENUM-401", "유효하지 않은 Enum 값입니다.", BAD_REQUEST),

    /**
     * File I/O
     */
    FILE_ENCODING_FAILED("FILE-301", "파일 이름 인코딩에 실패했습니다.", BAD_REQUEST),
    FILE_CREATE_FAILED("FILE-302", "파일 생성에 실패했습니다.", INTERNAL_SERVER_ERROR),
    INVALID_PHOTO_DATA("FILE-303", "사진 또는 순서 정보가 제공되지 않았습니다.", BAD_REQUEST),
    PHOTO_ORDER_MISMATCH("FILE-304", "사진의 개수와 순서 정보의 개수가 일치하지 않습니다.", BAD_REQUEST),
    FILE_SAVE_FAILED("FILE-305", "파일 저장에 실패했습니다.", INTERNAL_SERVER_ERROR),
    FILE_UPLOAD_FAILED("FILE-306", "파일 업로드에 실패했습니다.", BAD_REQUEST),
    FILE_DELETE_FAILED("FILE-307", "파일 삭제에 실패했습니다.", BAD_REQUEST),
    MAXIMUM_FILE_LIMIT_EXCEEDED("FILE-308", "업로드 가능한 갯수를 초과했습니다.", BAD_REQUEST),
    INVALID_FILE_NAME("FILE-309", "파일 이름이 유효하지 않습니다.", BAD_REQUEST),
    MISSING_FILE_EXTENSION("FILE-310", "파일 확장자가 없습니다.", BAD_REQUEST),
    UNSUPPORTED_FILE_EXTENSION("FILE-311", "지원하지 않는 파일 확장자입니다.", BAD_REQUEST),
    FILE_VALIDATION_FAILED("FILE-312", "파일 유효성 검사 실패", BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus status;
}

