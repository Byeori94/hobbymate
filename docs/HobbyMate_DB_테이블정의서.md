# HobbyMate DB 테이블 정의서

> DBMS: MariaDB  
> 문자셋: `utf8mb4`  
> 테이블 접두사: `HM_`  
> 테이블명과 컬럼명은 모두 대문자로 작성한다.

## 1. 설계 원칙

- PK는 `BIGINT UNSIGNED AUTO_INCREMENT`를 사용한다.
- 모든 테이블은 `InnoDB` 엔진을 사용한다.
- 날짜·시간은 `DATETIME`을 사용한다.
- 상태값과 유형값은 영문 대문자 코드로 저장한다.
- 삭제 데이터는 바로 물리 삭제하지 않고 상태값과 삭제 일시로 관리한다.
- 블로그형 게시글 본문은 `LONGTEXT`에 HTML 형태로 저장한다.
- 게시글당 이미지 최대 25장 제한은 서비스 계층에서 검증한다.
- 모임 내부 게시판 접근, 건의·신고 비공개 조회 등 권한은 Spring Security와 서비스 계층에서 검증한다.

## 2. 전체 테이블 목록

| 구분 | 테이블명 | 설명 |
|---|---|---|
| 회원 | `HM_MEMBER` | HobbyMate 회원 |
| 카테고리 | `HM_CATEGORY` | 취미 카테고리 |
| 모임 | `HM_CLUB` | 지속적으로 운영되는 취미 모임 |
| 모임 회원 | `HM_CLUB_MEMBER` | 모임 가입 회원과 모임 내 역할 |
| 가입 신청 | `HM_CLUB_JOIN_REQUEST` | 승인제 모임 가입 신청 |
| 만남 | `HM_MEETING` | 모임 안에서 열리는 대면·비대면 만남 |
| 만남 참여 | `HM_MEETING_PARTICIPANT` | 만남 참여 신청과 참석 상태 |
| 모임 게시글 | `HM_CLUB_POST` | 모임 전용 자유·만남모집·만남후기 게시글 |
| 모임 게시글 이미지 | `HM_CLUB_POST_IMAGE` | 모임 게시글 본문 이미지 |
| 모임 댓글 | `HM_CLUB_COMMENT` | 모임 게시글 댓글·대댓글 |
| 전체 게시글 | `HM_BOARD_POST` | 서비스 전체 공지사항·자유게시글 |
| 전체 게시글 이미지 | `HM_BOARD_POST_IMAGE` | 전체 게시글 본문 이미지 |
| 전체 댓글 | `HM_BOARD_COMMENT` | 전체 게시글 댓글·대댓글 |
| 건의 | `HM_SUGGESTION` | 작성자 본인과 관리자만 조회 가능한 건의 |
| 건의 답변 | `HM_SUGGESTION_REPLY` | 관리자 건의 답변 |
| 신고 | `HM_REPORT` | 작성자 본인과 관리자만 조회 가능한 신고 |
| 관리자 처리 이력 | `HM_ADMIN_ACTION_HISTORY` | 관리자의 주요 상태 변경 및 처리 이력 |

## 3. 코드값 정의

### 3.1 성별

| 값 | 설명 |
|---|---|
| `FEMALE` | 여성 |
| `MALE` | 남성 |

> `FEMAILE`이 아니라 `FEMALE`이 정확한 표기다.

### 3.2 Y/N

| 값 | 설명 |
|---|---|
| `Y` | 예, 사용, 인증, 활성 |
| `N` | 아니오, 미사용, 미인증, 비활성 |

### 3.3 회원 권한

| 값 | 설명 |
|---|---|
| `USER` | 일반 회원 |
| `ADMIN` | 플랫폼 관리자 |

### 3.4 회원 상태

| 값 | 설명 |
|---|---|
| `ACTIVE` | 정상 이용 중 |
| `SUSPENDED` | 관리자에 의해 이용 정지 |
| `WITHDRAWN` | 회원 탈퇴 |

### 3.5 모임 모집 성별

| 값 | 설명 |
|---|---|
| `FEMALE` | 여성 회원만 가입 가능 |
| `MALE` | 남성 회원만 가입 가능 |
| `MIXED` | 성별 제한 없는 혼성 모임 |

### 3.6 모임 가입 방식

| 값 | 설명 |
|---|---|
| `IMMEDIATE` | 조건 충족 시 즉시 가입 |
| `APPROVAL` | 모임장 또는 운영진 승인 후 가입 |

### 3.7 모임 상태

| 값 | 설명 |
|---|---|
| `ACTIVE` | 정상 운영 중 |
| `CLOSED` | 모임 폐쇄 |
| `BLOCKED` | 플랫폼 관리자에 의해 운영 중지 |

### 3.8 모임 모집 상태

| 값 | 설명 |
|---|---|
| `OPEN` | 신규 회원 모집 중 |
| `FULL` | 정원 도달로 모집 마감 |
| `CLOSED` | 모임장이 모집 종료 |

### 3.9 모임 내 역할

| 값 | 설명 |
|---|---|
| `LEADER` | 모임장 |
| `MANAGER` | 모임 운영진 |
| `MEMBER` | 일반 모임 회원 |

### 3.10 모임 회원 상태

| 값 | 설명 |
|---|---|
| `ACTIVE` | 현재 활동 중 |
| `WITHDRAWN` | 자진 탈퇴 |
| `KICKED` | 모임장 또는 운영진에 의해 강퇴 |

### 3.11 가입 신청 상태

| 값 | 설명 |
|---|---|
| `WAITING` | 승인 대기 |
| `APPROVED` | 승인 |
| `REJECTED` | 거절 |
| `CANCELED` | 신청자가 신청 취소 |

### 3.12 만남 유형

| 값 | 설명 |
|---|---|
| `OFFLINE` | 실제 장소에서 만나는 대면 만남 |
| `ONLINE` | Zoom 등 외부 화상회의 도구를 이용하는 비대면 만남 |

### 3.13 지도 제공사

| 값 | 설명 |
|---|---|
| `KAKAO` | 카카오 지도 |
| `NAVER` | 네이버 지도 |

### 3.14 화상회의 플랫폼

| 값 | 설명 |
|---|---|
| `ZOOM` | Zoom |
| `GOOGLE_MEET` | Google Meet |
| `MICROSOFT_TEAMS` | Microsoft Teams |
| `DISCORD` | Discord |
| `OTHER` | 그 외 플랫폼 |

### 3.15 만남 상태

| 값 | 설명 |
|---|---|
| `RECRUITING` | 참여자 모집 중 |
| `CLOSED` | 참여 모집 마감 |
| `COMPLETED` | 만남 완료 |
| `CANCELED` | 만남 취소 |

### 3.16 만남 참여 상태

| 값 | 설명 |
|---|---|
| `APPLIED` | 참여 신청 |
| `CONFIRMED` | 참여 확정 |
| `CANCELED` | 참여 취소 |
| `ATTENDED` | 실제 참석 |
| `ABSENT` | 불참 |

### 3.17 모임 게시글 유형

| 값 | 설명 |
|---|---|
| `FREE` | 모임 자유게시판 |
| `MEETING_RECRUIT` | 만남 모집 게시판 |
| `MEETING_REVIEW` | 만남 후기 게시판 |

### 3.18 전체 게시판 유형

| 값 | 설명 |
|---|---|
| `NOTICE` | 공지사항 |
| `FREE` | 서비스 전체 자유게시판 |

### 3.19 게시글·댓글 상태

| 값 | 설명 |
|---|---|
| `ACTIVE` | 정상 노출 |
| `DELETED` | 삭제 |
| `BLOCKED` | 관리자 차단 |

### 3.20 이미지 정렬

| 값 | 설명 |
|---|---|
| `LEFT` | 왼쪽 정렬 |
| `CENTER` | 가운데 정렬 |
| `RIGHT` | 오른쪽 정렬 |

### 3.21 이미지 상태

| 값 | 설명 |
|---|---|
| `TEMP` | 게시글 저장 전 임시 업로드 |
| `USED` | 저장된 게시글에서 사용 중 |
| `DELETED` | 삭제된 이미지 |

### 3.22 건의 처리 상태

| 값 | 설명 |
|---|---|
| `RECEIVED` | 접수 |
| `IN_PROGRESS` | 처리 중 |
| `COMPLETED` | 처리 완료 |
| `REJECTED` | 반려 |

### 3.23 신고 대상

| 값 | 설명 |
|---|---|
| `MEMBER` | 회원 |
| `CLUB` | 모임 |
| `CLUB_POST` | 모임 내부 게시글 |
| `CLUB_COMMENT` | 모임 내부 댓글 |
| `BOARD_POST` | 서비스 전체 게시글 |
| `BOARD_COMMENT` | 서비스 전체 댓글 |
| `MEETING` | 모임 내 만남 |

### 3.24 신고 사유

| 값 | 설명 |
|---|---|
| `SPAM` | 광고·도배·스팸 |
| `ABUSE` | 욕설·비방·괴롭힘 |
| `INAPPROPRIATE` | 부적절한 콘텐츠 |
| `FRAUD` | 사기 또는 금전 피해 우려 |
| `PRIVACY` | 개인정보 침해 |
| `OTHER` | 기타 |

### 3.25 신고 처리 상태

| 값 | 설명 |
|---|---|
| `RECEIVED` | 신고 접수 |
| `REVIEWING` | 관리자 검토 중 |
| `RESOLVED` | 처리 완료 |
| `REJECTED` | 신고 반려 |

## 4. 테이블 상세

### 4.1 `HM_MEMBER`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `MEMBER_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 회원 PK |
| `LOGIN_ID` | VARCHAR(50) | N |  | 일반 로그인 아이디, UNIQUE |
| `PASSWORD` | VARCHAR(255) | N |  | 암호화 비밀번호 |
| `NAME` | VARCHAR(50) | N |  | 본인인증 이름 |
| `NICKNAME` | VARCHAR(50) | N |  | 닉네임, UNIQUE |
| `EMAIL` | VARCHAR(255) | N |  | 이메일, UNIQUE |
| `PHONE` | VARCHAR(30) | N |  | 휴대폰 번호 |
| `BIRTH_DATE` | DATE | N |  | 본인인증 생년월일 |
| `GENDER` | VARCHAR(10) | N |  | `FEMALE`, `MALE` |
| `IDENTITY_VERIFIED_YN` | CHAR(1) | N | `N` | 본인인증 여부 |
| `IDENTITY_VERIFIED_AT` | DATETIME | Y | NULL | 본인인증 완료 일시 |
| `CI_HASH` | VARCHAR(255) | Y | NULL | 중복가입 확인용 CI 해시 |
| `PROFILE_IMAGE_URL` | VARCHAR(1000) | Y | NULL | 프로필 이미지 URL |
| `MEMBER_ROLE` | VARCHAR(20) | N | `USER` | `USER`, `ADMIN` |
| `MEMBER_STATUS` | VARCHAR(20) | N | `ACTIVE` | `ACTIVE`, `SUSPENDED`, `WITHDRAWN` |
| `WITHDRAWN_AT` | DATETIME | Y | NULL | 탈퇴 일시 |
| `CREATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 등록 일시 |
| `UPDATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 수정 일시 |

### 4.2 `HM_CATEGORY`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `CATEGORY_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 카테고리 PK |
| `PARENT_CATEGORY_ID` | BIGINT UNSIGNED | Y | NULL | 상위 카테고리 FK |
| `CATEGORY_NAME` | VARCHAR(100) | N |  | 카테고리명 |
| `DISPLAY_ORDER` | INT | N | `0` | 노출 순서 |
| `USE_YN` | CHAR(1) | N | `Y` | 사용 여부 |
| `CREATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 등록 일시 |
| `UPDATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 수정 일시 |

### 4.3 `HM_CLUB`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `CLUB_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 모임 PK |
| `OWNER_MEMBER_ID` | BIGINT UNSIGNED | N |  | 모임장 회원 FK |
| `CATEGORY_ID` | BIGINT UNSIGNED | N |  | 카테고리 FK |
| `CLUB_NAME` | VARCHAR(150) | N |  | 모임명 |
| `CLUB_SUMMARY` | VARCHAR(500) | N |  | 한줄 소개 |
| `CLUB_DESCRIPTION` | LONGTEXT | N |  | 모임 상세 소개 HTML |
| `REPRESENTATIVE_IMAGE_URL` | VARCHAR(1000) | Y | NULL | 대표 이미지 |
| `ACTIVITY_REGION` | VARCHAR(200) | Y | NULL | 주 활동 지역 |
| `GENDER_POLICY` | VARCHAR(10) | N | `MIXED` | `FEMALE`, `MALE`, `MIXED` |
| `MIN_AGE` | SMALLINT UNSIGNED | Y | NULL | 가입 최소 만 나이 |
| `MAX_AGE` | SMALLINT UNSIGNED | Y | NULL | 가입 최대 만 나이 |
| `MAX_MEMBER_COUNT` | INT UNSIGNED | N |  | 최대 인원 |
| `JOIN_TYPE` | VARCHAR(20) | N | `APPROVAL` | `IMMEDIATE`, `APPROVAL` |
| `CLUB_STATUS` | VARCHAR(20) | N | `ACTIVE` | 모임 상태 |
| `RECRUIT_STATUS` | VARCHAR(20) | N | `OPEN` | 모집 상태 |
| `CREATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 등록 일시 |
| `UPDATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 수정 일시 |
| `CLOSED_AT` | DATETIME | Y | NULL | 폐쇄 일시 |

### 4.4 `HM_CLUB_MEMBER`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `CLUB_MEMBER_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 모임 회원 PK |
| `CLUB_ID` | BIGINT UNSIGNED | N |  | 모임 FK |
| `MEMBER_ID` | BIGINT UNSIGNED | N |  | 회원 FK |
| `CLUB_ROLE` | VARCHAR(20) | N | `MEMBER` | `LEADER`, `MANAGER`, `MEMBER` |
| `MEMBER_STATUS` | VARCHAR(20) | N | `ACTIVE` | `ACTIVE`, `WITHDRAWN`, `KICKED` |
| `JOINED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 가입 일시 |
| `LEFT_AT` | DATETIME | Y | NULL | 탈퇴·강퇴 일시 |

### 4.5 `HM_CLUB_JOIN_REQUEST`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `JOIN_REQUEST_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 가입 신청 PK |
| `CLUB_ID` | BIGINT UNSIGNED | N |  | 모임 FK |
| `MEMBER_ID` | BIGINT UNSIGNED | N |  | 신청 회원 FK |
| `REQUEST_MESSAGE` | VARCHAR(1000) | Y | NULL | 가입 신청 메시지 |
| `REQUEST_STATUS` | VARCHAR(20) | N | `WAITING` | 신청 상태 |
| `PROCESSED_BY` | BIGINT UNSIGNED | Y | NULL | 처리자 회원 FK |
| `PROCESSED_AT` | DATETIME | Y | NULL | 처리 일시 |
| `REJECT_REASON` | VARCHAR(1000) | Y | NULL | 거절 사유 |
| `CREATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 신청 일시 |
| `UPDATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 수정 일시 |

### 4.6 `HM_MEETING`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `MEETING_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 만남 PK |
| `CLUB_ID` | BIGINT UNSIGNED | N |  | 소속 모임 FK |
| `CREATED_BY` | BIGINT UNSIGNED | N |  | 등록자 회원 FK |
| `MEETING_TITLE` | VARCHAR(200) | N |  | 만남명 |
| `MEETING_TYPE` | VARCHAR(20) | N |  | `OFFLINE`, `ONLINE` |
| `START_DATETIME` | DATETIME | N |  | 시작 일시 |
| `END_DATETIME` | DATETIME | N |  | 종료 일시 |
| `APPLY_END_DATETIME` | DATETIME | N |  | 참여 신청 마감 일시 |
| `MAX_MEMBER_COUNT` | INT UNSIGNED | N |  | 최대 참여 인원 |
| `PARTICIPATION_FEE` | DECIMAL(12,2) | N | `0` | 참가비 |
| `PREPARATION` | VARCHAR(1000) | Y | NULL | 준비물 |
| `MEETING_STATUS` | VARCHAR(20) | N | `RECRUITING` | 만남 상태 |
| `PLACE_NAME` | VARCHAR(255) | Y | NULL | 대면 장소명 |
| `ROAD_ADDRESS` | VARCHAR(500) | Y | NULL | 도로명 주소 |
| `JIBUN_ADDRESS` | VARCHAR(500) | Y | NULL | 지번 주소 |
| `DETAIL_ADDRESS` | VARCHAR(500) | Y | NULL | 상세 주소 |
| `LATITUDE` | DECIMAL(10,7) | Y | NULL | 위도 |
| `LONGITUDE` | DECIMAL(10,7) | Y | NULL | 경도 |
| `MAP_PROVIDER` | VARCHAR(20) | Y | NULL | `KAKAO`, `NAVER` |
| `MAP_PLACE_ID` | VARCHAR(255) | Y | NULL | 지도 API 장소 ID |
| `VIDEO_PLATFORM` | VARCHAR(30) | Y | NULL | 화상회의 플랫폼 |
| `MEETING_URL` | VARCHAR(2000) | Y | NULL | 비대면 참여 링크 |
| `MEETING_PASSWORD` | VARCHAR(255) | Y | NULL | 회의 비밀번호 |
| `ONLINE_GUIDE` | VARCHAR(2000) | Y | NULL | 접속 안내 |
| `CREATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 등록 일시 |
| `UPDATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 수정 일시 |
| `CANCELED_AT` | DATETIME | Y | NULL | 취소 일시 |

- `OFFLINE` 만남은 장소명, 주소, 위도, 경도를 필수로 검증한다.
- `ONLINE` 만남은 화상회의 플랫폼과 참여 링크를 필수로 검증한다.
- 비대면 참여 링크는 해당 만남 참여자에게만 노출한다.

### 4.7 `HM_MEETING_PARTICIPANT`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `MEETING_PARTICIPANT_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 참여자 PK |
| `MEETING_ID` | BIGINT UNSIGNED | N |  | 만남 FK |
| `MEMBER_ID` | BIGINT UNSIGNED | N |  | 참여 회원 FK |
| `PARTICIPATION_STATUS` | VARCHAR(20) | N | `APPLIED` | 참여 상태 |
| `APPLIED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 신청 일시 |
| `CANCELED_AT` | DATETIME | Y | NULL | 취소 일시 |
| `CONFIRMED_AT` | DATETIME | Y | NULL | 확정 일시 |
| `ATTENDANCE_PROCESSED_AT` | DATETIME | Y | NULL | 출석 처리 일시 |

### 4.8 `HM_CLUB_POST`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `CLUB_POST_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 모임 게시글 PK |
| `CLUB_ID` | BIGINT UNSIGNED | N |  | 모임 FK |
| `AUTHOR_MEMBER_ID` | BIGINT UNSIGNED | N |  | 작성자 회원 FK |
| `MEETING_ID` | BIGINT UNSIGNED | Y | NULL | 연결 만남 FK |
| `POST_TYPE` | VARCHAR(30) | N |  | `FREE`, `MEETING_RECRUIT`, `MEETING_REVIEW` |
| `TITLE` | VARCHAR(300) | N |  | 제목 |
| `CONTENT` | LONGTEXT | N |  | 블로그형 HTML 본문 |
| `VIEW_COUNT` | BIGINT UNSIGNED | N | `0` | 조회수 |
| `NOTICE_YN` | CHAR(1) | N | `N` | 모임 내 상단 고정 여부 |
| `POST_STATUS` | VARCHAR(20) | N | `ACTIVE` | 게시글 상태 |
| `CREATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 등록 일시 |
| `UPDATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 수정 일시 |
| `DELETED_AT` | DATETIME | Y | NULL | 삭제 일시 |

### 4.9 `HM_CLUB_POST_IMAGE`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `CLUB_POST_IMAGE_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 이미지 PK |
| `CLUB_POST_ID` | BIGINT UNSIGNED | Y | NULL | 게시글 FK, 임시 업로드 시 NULL |
| `UPLOAD_TOKEN` | VARCHAR(100) | Y | NULL | 임시 업로드 토큰 |
| `ORIGINAL_FILE_NAME` | VARCHAR(500) | N |  | 원본 파일명 |
| `STORED_FILE_NAME` | VARCHAR(500) | N |  | 저장 파일명 |
| `FILE_URL` | VARCHAR(2000) | N |  | 이미지 URL |
| `FILE_SIZE` | BIGINT UNSIGNED | N |  | 파일 크기(byte) |
| `CONTENT_TYPE` | VARCHAR(100) | N |  | MIME 타입 |
| `IMAGE_ORDER` | INT UNSIGNED | N | `0` | 이미지 순서 |
| `ALIGN_TYPE` | VARCHAR(10) | N | `CENTER` | `LEFT`, `CENTER`, `RIGHT` |
| `IMAGE_STATUS` | VARCHAR(20) | N | `TEMP` | `TEMP`, `USED`, `DELETED` |
| `CREATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 등록 일시 |
| `UPDATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 수정 일시 |

> 글당 최대 25장은 서비스 계층에서 검증한다.

### 4.10 `HM_CLUB_COMMENT`

- `PARENT_COMMENT_ID`가 NULL이면 일반 댓글, 값이 있으면 대댓글이다.
- 상태값은 `ACTIVE`, `DELETED`, `BLOCKED`를 사용한다.

### 4.11 `HM_BOARD_POST`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `BOARD_POST_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 전체 게시글 PK |
| `AUTHOR_MEMBER_ID` | BIGINT UNSIGNED | N |  | 작성자 회원 FK |
| `BOARD_TYPE` | VARCHAR(20) | N |  | `NOTICE`, `FREE` |
| `TITLE` | VARCHAR(300) | N |  | 제목 |
| `CONTENT` | LONGTEXT | N |  | HTML 본문 |
| `VIEW_COUNT` | BIGINT UNSIGNED | N | `0` | 조회수 |
| `TOP_FIXED_YN` | CHAR(1) | N | `N` | 상단 고정 여부 |
| `COMMENT_USE_YN` | CHAR(1) | N | `Y` | 댓글 사용 여부 |
| `POST_STATUS` | VARCHAR(20) | N | `ACTIVE` | 게시글 상태 |
| `CREATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 등록 일시 |
| `UPDATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 수정 일시 |
| `DELETED_AT` | DATETIME | Y | NULL | 삭제 일시 |

- 공지사항 작성·수정·삭제는 관리자만 가능하다.
- 전체 자유게시판은 로그인 회원이 작성한다.
- 공지사항은 `COMMENT_USE_YN = N`으로 댓글을 막을 수 있다.

### 4.12 `HM_BOARD_POST_IMAGE`

`HM_CLUB_POST_IMAGE`와 같은 구조로 전체 게시글의 블로그형 본문 이미지를 관리한다.

### 4.13 `HM_BOARD_COMMENT`

- `PARENT_COMMENT_ID`가 NULL이면 일반 댓글, 값이 있으면 대댓글이다.
- 상태값은 `ACTIVE`, `DELETED`, `BLOCKED`를 사용한다.

### 4.14 `HM_SUGGESTION`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `SUGGESTION_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 건의 PK |
| `AUTHOR_MEMBER_ID` | BIGINT UNSIGNED | N |  | 작성자 회원 FK |
| `TITLE` | VARCHAR(300) | N |  | 제목 |
| `CONTENT` | LONGTEXT | N |  | 건의 내용 |
| `SUGGESTION_STATUS` | VARCHAR(20) | N | `RECEIVED` | 처리 상태 |
| `PROCESSED_BY` | BIGINT UNSIGNED | Y | NULL | 처리 관리자 FK |
| `PROCESSED_AT` | DATETIME | Y | NULL | 처리 일시 |
| `CREATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 등록 일시 |
| `UPDATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 수정 일시 |

작성자 본인과 플랫폼 관리자만 조회할 수 있다.

### 4.15 `HM_SUGGESTION_REPLY`

한 건의에 여러 번 답변할 수 있도록 `HM_SUGGESTION`과 1:N 관계로 설계했다.

### 4.16 `HM_REPORT`

| 컬럼명 | 자료형 | NULL | 기본값 | 설명 |
|---|---|---:|---|---|
| `REPORT_ID` | BIGINT UNSIGNED | N | AUTO_INCREMENT | 신고 PK |
| `REPORTER_MEMBER_ID` | BIGINT UNSIGNED | N |  | 신고자 회원 FK |
| `TARGET_TYPE` | VARCHAR(30) | N |  | 신고 대상 유형 |
| `TARGET_ID` | BIGINT UNSIGNED | N |  | 신고 대상 PK 값 |
| `REPORT_REASON` | VARCHAR(30) | N |  | 신고 사유 |
| `REPORT_CONTENT` | LONGTEXT | N |  | 상세 내용 |
| `REPORT_STATUS` | VARCHAR(20) | N | `RECEIVED` | 처리 상태 |
| `PROCESSED_BY` | BIGINT UNSIGNED | Y | NULL | 처리 관리자 FK |
| `PROCESSED_AT` | DATETIME | Y | NULL | 처리 일시 |
| `PROCESS_RESULT` | LONGTEXT | Y | NULL | 처리 결과 |
| `CREATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 등록 일시 |
| `UPDATED_AT` | DATETIME | N | CURRENT_TIMESTAMP | 수정 일시 |

`TARGET_ID`는 여러 종류의 테이블 PK를 가리키므로 물리 FK를 설정하지 않는다. `TARGET_TYPE`과 `TARGET_ID`의 실제 유효성은 서비스 계층에서 검증한다.

## 5. 핵심 관계

```text
HM_MEMBER
 ├─ 1:N HM_CLUB
 ├─ 1:N HM_CLUB_MEMBER
 ├─ 1:N HM_CLUB_JOIN_REQUEST
 ├─ 1:N HM_MEETING_PARTICIPANT
 ├─ 1:N HM_CLUB_POST
 ├─ 1:N HM_BOARD_POST
 ├─ 1:N HM_SUGGESTION
 └─ 1:N HM_REPORT

HM_CLUB
 ├─ 1:N HM_CLUB_MEMBER
 ├─ 1:N HM_CLUB_JOIN_REQUEST
 ├─ 1:N HM_MEETING
 └─ 1:N HM_CLUB_POST

HM_MEETING
 ├─ 1:N HM_MEETING_PARTICIPANT
 └─ 1:N HM_CLUB_POST

HM_CLUB_POST
 ├─ 1:N HM_CLUB_POST_IMAGE
 └─ 1:N HM_CLUB_COMMENT

HM_BOARD_POST
 ├─ 1:N HM_BOARD_POST_IMAGE
 └─ 1:N HM_BOARD_COMMENT

HM_SUGGESTION
 └─ 1:N HM_SUGGESTION_REPLY

HM_MEMBER
 └─ 1:N HM_ADMIN_ACTION_HISTORY
      관리자 처리자 기준
```

## 6. 서비스 계층 검증 규칙

1. 본인인증 완료 회원만 모임에 가입할 수 있다.
2. 모임 가입 시 회원 성별과 `GENDER_POLICY`를 비교한다.
3. `BIRTH_DATE`를 기준으로 만 나이를 계산하여 `MIN_AGE`, `MAX_AGE`를 검증한다.
4. 현재 활동 회원 수와 `MAX_MEMBER_COUNT`를 트랜잭션 안에서 검증한다.
5. 모임 가입자만 모임 내부 게시판에 접근할 수 있다.
6. 모임장·운영진만 가입 신청을 승인 또는 거절한다.
7. 만남 정원을 초과하지 않도록 동시성 검증을 수행한다.
8. `OFFLINE` 만남은 장소·주소·위도·경도가 필수다.
9. `ONLINE` 만남은 화상회의 플랫폼과 참여 링크가 필수다.
10. 온라인 참여 링크는 해당 만남 참여자에게만 노출한다.
11. 만남 후기 작성자를 실제 참석자(`ATTENDED`)로 제한할 수 있다.
12. 게시글당 사용 중인 이미지는 최대 25장이다.
13. 오래된 `TEMP` 이미지는 배치 작업으로 삭제한다.
14. 건의와 신고는 작성자 본인 및 관리자만 조회할 수 있다.
15. 공지사항 작성·수정·삭제는 관리자만 가능하다.
16. 회원 정지, 모임 차단, 게시글·댓글 차단/복원, 신고·건의 처리 등 주요 관리자 작업은 `HM_ADMIN_ACTION_HISTORY`에 기록한다.
17. 관리자 대상 데이터 변경과 이력 저장은 같은 트랜잭션으로 처리한다.

## 7. 개인정보·보안 주의사항

- 주민등록번호는 저장하지 않는다.
- 휴대폰 번호는 암호화 또는 마스킹 저장을 검토한다.
- CI는 원문보다 해시 저장을 권장한다.
- 비밀번호는 BCrypt 등 단방향 해시로 저장한다.
- 화상회의 링크와 비밀번호는 권한 검증 후 응답한다.
- HTML 본문은 XSS 방지를 위해 허용 태그 기준으로 정제한다.
