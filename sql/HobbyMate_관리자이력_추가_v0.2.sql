-- HobbyMate 관리자 처리 이력 테이블 추가
-- DBMS: MariaDB
-- 문자셋: utf8mb4
-- DBeaver 실행: Alt + X

CREATE TABLE IF NOT EXISTS HM_ADMIN_ACTION_HISTORY (
    ADMIN_ACTION_HISTORY_ID BIGINT UNSIGNED NOT NULL AUTO_INCREMENT
        COMMENT '관리자 처리 이력 PK',

    ADMIN_MEMBER_ID BIGINT UNSIGNED NOT NULL
        COMMENT '처리 관리자 회원 FK',

    TARGET_TYPE VARCHAR(30) NOT NULL
        COMMENT '대상: MEMBER, CLUB, CLUB_POST, CLUB_COMMENT, BOARD_POST, BOARD_COMMENT, REPORT, SUGGESTION, CATEGORY, MEETING',

    TARGET_ID BIGINT UNSIGNED NOT NULL
        COMMENT '처리 대상 테이블의 PK 값',

    ACTION_TYPE VARCHAR(30) NOT NULL
        COMMENT '처리: SUSPEND, UNSUSPEND, BLOCK, UNBLOCK, DELETE, RESTORE, APPROVE, REJECT, COMPLETE, UPDATE',

    ACTION_REASON VARCHAR(2000) NULL
        COMMENT '처리 사유',

    BEFORE_STATUS VARCHAR(50) NULL
        COMMENT '처리 전 상태 코드',

    AFTER_STATUS VARCHAR(50) NULL
        COMMENT '처리 후 상태 코드',

    ACTION_RESULT VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'
        COMMENT '처리 결과: SUCCESS, FAIL, CANCELED',

    RESULT_MESSAGE VARCHAR(2000) NULL
        COMMENT '처리 결과 또는 참고 메모',

    BEFORE_VERIFICATION_METHOD VARCHAR(30) NULL
        COMMENT '처리 전 본인인증 방식',

    AFTER_VERIFICATION_METHOD VARCHAR(30) NULL
        COMMENT '처리 후 본인인증 방식',

    BEFORE_CI_HASH VARCHAR(255) NULL
        COMMENT '처리 전 CI 해시(관리자 화면 및 로그 노출 금지)',

    AFTER_CI_HASH VARCHAR(255) NULL
        COMMENT '처리 후 CI 해시(관리자 화면 및 로그 노출 금지)',

    PROCESSING_TYPE VARCHAR(20) NULL
        COMMENT '처리 단위: INDIVIDUAL 개별, BATCH 일괄',

    OPERATION_ID VARCHAR(36) NULL
        COMMENT '동일 일괄 요청 식별 UUID',

    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        COMMENT '처리 일시',

    PRIMARY KEY (ADMIN_ACTION_HISTORY_ID),

    KEY IX_HM_ADMIN_ACTION_HISTORY_ADMIN_DATE
        (ADMIN_MEMBER_ID, CREATED_AT),

    KEY IX_HM_ADMIN_ACTION_HISTORY_TARGET
        (TARGET_TYPE, TARGET_ID, CREATED_AT),

    KEY IX_HM_ADMIN_ACTION_HISTORY_ACTION
        (ACTION_TYPE, CREATED_AT),

    KEY IX_HM_ADMIN_ACTION_HISTORY_OPERATION
        (OPERATION_ID),

    CONSTRAINT FK_HM_ADMIN_ACTION_HISTORY_ADMIN
        FOREIGN KEY (ADMIN_MEMBER_ID)
        REFERENCES HM_MEMBER (MEMBER_ID),

    CONSTRAINT CK_HM_ADMIN_ACTION_HISTORY_TARGET_TYPE
        CHECK (
            TARGET_TYPE IN (
                'MEMBER',
                'CLUB',
                'CLUB_POST',
                'CLUB_COMMENT',
                'BOARD_POST',
                'BOARD_COMMENT',
                'REPORT',
                'SUGGESTION',
                'CATEGORY',
                'MEETING'
            )
        ),

    CONSTRAINT CK_HM_ADMIN_ACTION_HISTORY_ACTION_TYPE
        CHECK (
            ACTION_TYPE IN (
                'SUSPEND',
                'UNSUSPEND',
                'BLOCK',
                'UNBLOCK',
                'DELETE',
                'RESTORE',
                'APPROVE',
                'REJECT',
                'COMPLETE',
                'UPDATE',
                'ADMIN_TEMP_AUTH',
                'ADMIN_TEMP_AUTH_CANCEL',
                'ADMIN_TEMP_AUTH_UPDATE'
            )
        ),

    CONSTRAINT CK_HM_ADMIN_ACTION_HISTORY_ACTION_RESULT
        CHECK (
            ACTION_RESULT IN (
                'SUCCESS',
                'FAIL',
                'CANCELED'
            )
        ),

    CONSTRAINT CK_HM_ADMIN_ACTION_HISTORY_PROCESSING_TYPE
        CHECK (
            PROCESSING_TYPE IS NULL
            OR PROCESSING_TYPE IN ('INDIVIDUAL', 'BATCH')
        )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci
  COMMENT='플랫폼 관리자 주요 처리 이력';
