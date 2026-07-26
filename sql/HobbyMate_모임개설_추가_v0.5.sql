-- HobbyMate 모임 개설 기능 최소 스키마 확장 v0.5
-- 대상: v0.4 전체 서비스 정책 확장을 아직 적용하지 않은 현재 개발 DB
-- 목적: 승인제 모임의 가입 신청 안내·질문 저장
-- v0.4 적용 여부와 관계없이 재실행할 수 있도록 IF NOT EXISTS를 사용한다.

SET NAMES utf8mb4;

ALTER TABLE HM_CLUB
    ADD COLUMN IF NOT EXISTS JOIN_GUIDE VARCHAR(1000) NULL
        COMMENT '승인제 가입 신청 안내·질문, 화면 입력 최대 500자'
        AFTER JOIN_TYPE;
