<p align="center">
  <img width="100%" alt="main-banner" src="https://github.com/user-attachments/assets/5e9f9dcd-717a-4f33-843b-4a1d985e53b4" />
</p>

<h1 align="center">동구라미</h1>
<p align="center">수원대학교 동아리 연합회 공식 플랫폼</p>

<p align="center">
  <a href="https://admin.donggurami.net">웹 서비스</a> ·
  <a href="https://apps.apple.com/kr/app/%EB%8F%99%EA%B5%AC%EB%9D%BC%EB%AF%B8/id6692607046">iOS</a> ·
  <a href="https://play.google.com/store/apps/details?id=com.usw.flag.temp.usw_circle_link">Android</a> ·
  <a href="https://linktr.ee/woochang4862">시연 영상</a>
</p>

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [성과](#2-성과)
3. [기술 스택](#3-기술-스택)
4. [시스템 설계](#4-시스템-설계)
5. [담당 영역](#5-담당-영역)
6. [테스트](#6-테스트)

---

## 1. 프로젝트 개요

동구라미는 수원대학교 동아리 연합회의 공식 통합 관리 플랫폼입니다. 기존에 카카오톡·엑셀로 분산 관리되던 동아리 정보와 지원 프로세스를 디지털화했습니다.  

- **기간:** 2024년 4월 ~ 2025년 3월 (약 12개월, 이후 후임 팀 인계)
- **팀 구성:** 백엔드 4명 / 웹 프론트엔드 4명 / 모바일(iOS·Android) 3명 / 디자인 1명

| 구분 | 성명 |
|------|------|
| BE (Web/Mobile) | 김지오, 남궁다연, 방혁, 한지형 |
| Design | 이보영 |
| FE (Web) | 이동수, 박성재, 노경미, 김수민 |
| FE (Mobile) | 정우창, 유지석, 이수빈 |

**해결한 문제**
- 동아리별 개별 홍보로 인한 정보 분산 → 단일 플랫폼에서 전체 동아리 탐색
- 종이/구글폼 기반 지원서 관리 → 실시간 지원 현황 조회 및 결과 알림(FCM)

**서비스 링크**

| 플랫폼 | 링크 | 상태 |
|--------|------|------|
| 웹 (권장) | [admin.donggurami.net](https://admin.donggurami.net) | 운영 중 |
| iOS | [App Store](https://apps.apple.com/kr/app/%EB%8F%99%EA%B5%AC%EB%9D%BC%EB%AF%B8/id6692607046) | 운영 중 |
| Android | [Google Play](https://play.google.com/store/apps/details?id=com.usw.flag.temp.usw_circle_link) | 운영 중 |

**주요 기능**

| 기능 | 설명 | 웹 | iOS | Android |
|------|------|:--:|:---:|:-------:|
| 동아리 탐색 | 카테고리·모집상태 필터 및 상세 정보 조회 | ✓ | ✓ | ✓ |
| 지원서 제출 | 중복 방지 및 실시간 지원 현황 확인 | ✓ | ✓ | ✓ |
| 합격 알림 | FCM 기반 합격/불합격 푸시 알림 | - | ✓ | ✓ |
| 회원 관리 | 엑셀 업로드 및 정회원/비회원 분류 | ✓ | ✓ | ✓ |
| 프로필 관리 | 프로필 수정 및 소속 동아리 조회 | ✓ | ✓ | ✓ |

---

## 2. 성과

App Store·Google Play 배포를 하고 동아리 연합회 공식 플랫폼으로 채택됐습니다.  

| 지표 | 수치 | 측정 시점 |
|------|------|-----------|
| 누적 가입자 수 | 약 200명 | 2024.09 출시 후 |

---

## 3. 기술 스택

### Backend
| 기술 | 선택 이유 |
|------|-----------|
| Spring Boot 3.3.1 | Spring Security·Data JPA·트랜잭션 추상화가 일관되게 통합되어 있어 역할별 인증 분리, 벌크 연산, afterCommit() 패턴을 별도 설정 없이 조합할 수 있었음 |
| Spring Security | 필터 기반 인증 체계를 역할별로 분리하기 위한 표준 프레임워크 |
| Spring Data JPA | 도메인 중심 설계와 JPQL 벌크 연산을 혼용해 유지보수성과 성능을 균형 있게 확보 |

### Database
| 기술 | 선택 이유 |
|------|-----------|
| MySQL 8.0 | 복잡한 조인·페이징·트랜잭션이 필요한 관계형 데이터에 적합 |
| Redis | Refresh Token 저장소(TTL 자동 만료) 및 Rate Limiter(Bucket4j) 분산 상태 관리 |

### 인증 / 보안
| 기술 | 선택 이유 |
|------|-----------|
| JWT (jjwt) | Stateless 인증으로 수평 확장에 유리하고 role claim으로 토큰 검증 직후 인증 주체 확정 가능 |
| Bucket4j + Redis | 분산 환경에서도 동일한 Rate Limit 정책 적용 가능 |
| Jsoup | XSS 방지를 위한 입력 정제를 `@Sanitize` 어노테이션으로 선언적으로 적용 |

### 인프라
| 기술 | 선택 이유 |
|------|-----------|
| AWS EC2 | 서버 환경 제어 및 IAM 역할 기반 자격증명으로 키 없는 S3 접근 |
| AWS RDS | 자동 백업과 멀티 AZ로 데이터 내구성 확보 |
| AWS S3 + Presigned URL | 파일 트래픽을 서버가 직접 처리하지 않고 클라이언트가 S3와 직접 통신하도록 분리 |
| Nginx | 리버스 프록시 및 SSL 터미네이션 |

### 외부 연동
- Firebase FCM — 합격/불합격 결과 푸시 알림 (iOS·Android)
- Naver SMTP — 이메일 인증 / 아이디 찾기 / 비밀번호 재설정

---

## 4. 시스템 설계

### 서버 구성도
![Server Architecture](./docs/images/circle_link_arch.png)

- Prod·Test 환경을 EC2 인스턴스로 분리해 운영하며 각각 독립된 S3 버킷과 RDS를 사용
- 클라이언트(PC·Mobile) → CloudFront → S3 → EC2(Nginx) 순으로 트래픽이 흐르며 Nginx가 리버스 프록시와 SSL 터미네이션을 담당
- EC2에 IAM 역할을 부여해 액세스 키 없이 S3에 접근하도록 구성
- Redis는 EC2 내에서 Refresh Token 저장소와 Rate Limiter 상태 관리에 사용

### ERD
![ERD](./docs/images/circle_link_erd.png)

- Club이 중심 엔티티로, ClubIntro·ClubMainPhoto·ClubIntroPhoto·ClubMembers·ClubApplication이 Club에 연결되는 구조
- User와 Profile을 1:1로 분리해 인증 정보(User)와 프로필 정보(Profile)의 책임을 구분
- ClubApplication이 Profile과 Club을 잇는 다대일 구조로 지원서 중복을 DB 유니크 제약으로 보장

### API 명세서
- [[모바일(User) API]](https://documenter.getpostman.com/view/36800939/2sA3s1nrcY)
- [[웹(Leader/Admin) API]](https://documenter.getpostman.com/view/29405740/2sA3s6Doda)

---

## 5. 담당 영역

백엔드 팀원으로 인증/인가, 동아리·지원서·공지·Admin 기능, 공통 예외 처리, 파일 업로드 검증, 입력 정제를 담당했습니다.

### 인증/인가 구조

```
RoleBasedUserDetailsService (interface)
├── CustomUserDetailsService    → Role.USER
├── CustomLeaderDetailsService  → Role.LEADER
└── CustomAdminDetailsService   → Role.ADMIN
 
UserDetailsServiceManager
└── EnumMap<Role, RoleBasedUserDetailsService>  → O(1) 조회
```

- JWT Access Token에 `role` claim 포함 → 토큰 검증 직후 인증 대상 확정, JwtFilter에서 역할 판단 로직 제거
- Refresh Token은 `RefreshTokenStore`(Redis 읽기·쓰기) / `RefreshTokenCookieService`(쿠키 처리) / `RefreshTokenService`(발급·검증·재발급) 세 클래스로 책임 분리
- 로그아웃·비밀번호 변경 시 Redis에서 Refresh Token 즉시 무효화

### 동아리 관련 기능

- 동아리 목록 조회 시 메인 사진·해시태그·회원 수·리더를 일괄 조회해 쿼리 수를 최소화

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| 메인 사진·해시태그 조회 | 동아리당 개별 조회 (N+1) | `findByClubIds()` 일괄 조회 (2번) |
| 회원 수·리더 조회 | 페이지당 21번 (목록 1 + 동아리당 회원 수·리더 각 1씩 × 10) | 단일 집계 JPQL (1번) |

### 지원서 기능

- `ClubApplication` 엔티티에 `(profile_id, club_id)` 복합 유니크 제약 추가
- `saveAndFlush()`로 커밋 전 즉시 플러시 → `DataIntegrityViolationException`을 `ALREADY_APPLIED`로 변환

### 공지 기능

- 공지사항 CRUD (제목·내용·사진)
- 사진 삭제 시 `afterCommit()`으로 DB 커밋 이후에만 S3 삭제 실행
- 사진 순서(`order`) 필드로 업로드 순서 보장

### 동아리 연합회(관리자) 기능

- 동아리 생성 시 Club·Leader·ClubIntro·ClubMainPhoto·ClubIntroPhoto 기본 데이터 일괄 생성
- 동아리 삭제 시 JPQL 벌크 DELETE로 연관 테이블 9개를 테이블당 쿼리 1개로 처리 (265ms → 116ms, 약 56% 개선, 로컬 MySQL 기준)
- `afterCommit()`으로 DB 커밋 이후에만 S3 삭제 실행해 두 저장소 간 불일치 방지
- 동아리 카테고리 CRUD, 층별 사진 업로드·조회·삭제

### 파일 업로드 검증

- `FileSignatureValidator` — `@Component`로 분리해 `S3FileUploadService`·`ClubLeaderService`에 주입
- 파일 매직 바이트를 직접 읽어 확장자와 실제 포맷 일치 여부 검증, PNG는 스펙 기준 8바이트 시그니처 전체 검증
- S3 업로드는 Presigned URL 방식으로 처리해 파일 트래픽이 서버를 경유하지 않음

### 입력 검증 및 정제

- `@ValidClubRoomNumber` — 유효한 동아리방 번호 집합과 정규식을 결합한 커스텀 검증
- `@Sanitize` + `SanitizationBinder` — `@RequestBody`·`@RequestPart`·`WebDataBinder` 경로를 모두 커버하며 `@Sanitize`가 붙은 필드에만 선택적으로 Jsoup 정제 적용, `List<String>` 같은 컬렉션 필드도 원소 단위로 정제

| 구분 | 대상 |
|------|------|
| 적용 | 화면에 노출되는 자유 서술형 필드 (제목·본문·소개·이름·해시태그·카테고리명 등) |
| 비적용 | 비밀번호·토큰·UUID·이메일·학번·전화번호·계정 ID·URL·enum·숫자 등 정형 필드 |

### 공통 예외 처리

- `ApiResponse<T>` — 성공 응답 구조를 `message`·`data` 필드로 통일, `data`는 `@JsonInclude(NON_NULL)`로 null 시 미포함
- `@RestControllerAdvice`로 전역 예외를 한 곳에서 처리
- 운영 프로파일(`prod`)에서는 4xx 클라이언트 에러 로그를 출력하지 않아 노이즈 감소
- UUID 파싱 실패, JSON 역직렬화 오류, 파일 크기 초과 등 Spring 내부 예외도 일관된 형식으로 반환

---

## 6. 테스트

### 테스트 구성

총 **23개 테스트 클래스**, **191개 테스트 케이스**를 작성했습니다.

| 유형 | 클래스 수 | 주요 내용 |
|------|-----------|-----------|
| Service (단위) | 7 | Mockito 기반 비즈니스 로직·예외 분기 검증 |
| Repository (통합) | 4 | `@DataJpaTest` JPQL 커스텀 쿼리·벌크 DELETE 실행 검증 |
| Controller (슬라이스) | 3 | `@WebMvcTest` HTTP 상태코드·응답 본문 검증 |
| Security | 5 | JWT 발급·검증·만료, JwtFilter 동작, RefreshToken 생명주기 |
| Validator | 3 | 파일 시그니처 검증, 동아리방 번호 유효성, 입력 정제 |
| 기타 | 1 | `IntegrationAuthService` 로그아웃·토큰 갱신 흐름 |

### 테스트 전략

#### 단위 테스트(Service)

외부 의존성을 Mockito로 모킹해 비즈니스 로직에만 집중합니다. `MockedStatic`으로 `SecurityContextHolder`를 격리해 인증 컨텍스트를 제어합니다.

```java
// 예: 동시성 - DB 유니크 제약 위반을 도메인 예외로 변환
given(clubApplicationRepository.saveAndFlush(any()))
        .willThrow(new DataIntegrityViolationException("unique constraint"));

assertThatThrownBy(() -> service.submitClubApplication(clubUUID))
        .isInstanceOf(ClubApplicationException.class)
    .extracting(e -> ((ClubApplicationException) e).getExceptionType())
        .isEqualTo(ExceptionType.ALREADY_APPLIED);
```

#### 통합 테스트(Repository)

`@DataJpaTest`로 실제 JPA 쿼리를 검증합니다. `afterCommit()` 콜백 등록 여부와 S3 삭제 호출은 `AdminClubServiceTest`에서 `MockedStatic<TransactionSynchronizationManager>`를 활용해 분리 검증합니다.

```java
// 예: 커밋 후 S3 삭제 보장 검증
transactionMocked.verify(() -> TransactionSynchronizationManager
        .registerSynchronization(transactionSynchronizationCaptor.capture()));

        transactionSynchronizationCaptor.getValue().afterCommit();
verify(s3FileUploadService).deleteFiles(List.of("main.jpg", "intro-1.jpg", "intro-2.jpg"));
```

`afterCommit()`을 직접 트리거한 뒤 빈 문자열 key는 제외하고 유효한 S3 key만 `deleteFiles()`에 전달되는 것을 검증합니다.

#### 슬라이스 테스트(Controller)

`@WebMvcTest` + `addFilters = false`로 Security 필터를 제거하고 HTTP 레이어만 검증합니다. MockMvc로 상태코드, 응답 JSON 구조, 예외 코드를 단언합니다.

```java
mockMvc.perform(get("/apply/can-apply/{clubUUID}", clubUUID))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("지원 가능"))
        .andExpect(jsonPath("$.data").doesNotExist());
```

#### Security 테스트

유효·만료·변조 토큰 세 케이스를 각각 검증하고 JwtFilter에서 각 케이스에 따라 `CustomAuthenticationEntryPoint`가 올바른 에러 코드로 호출되는지 `ArgumentCaptor`로 확인합니다.
