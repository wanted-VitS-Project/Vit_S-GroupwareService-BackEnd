-- 입찰 AI 요약을 요청자 개인 초안과 회사 확정본으로 관리하고 비동기 처리할 수 있도록 확장한다.
-- 확정된 요약만 프로젝트 생성 근거로 연결하며, 프로젝트 생성 자체는 별도 명령에서 수행한다.
ALTER TABLE bid_notice_summary
    ADD COLUMN company_id BIGINT NULL
        COMMENT '요약을 소유한 회사(테넌트)'
        AFTER bid_notice_summary_id,
    ADD COLUMN project_id BIGINT NULL
        COMMENT '이 확정 요약을 근거로 생성한 프로젝트'
        AFTER company_id,
    ADD COLUMN notice_snapshot JSON NULL
        COMMENT '요약 요청 시점의 입찰 공고 및 첨부 메타데이터 스냅샷'
        AFTER prompt,
    ADD COLUMN overview_summary TEXT NULL
        COMMENT '공고 개요 요약'
        AFTER notice_snapshot,
    ADD COLUMN qualification_summary TEXT NULL
        COMMENT '참가 자격 요약'
        AFTER schedule_summary,
    ADD COLUMN processing_attempt_id CHAR(36) NULL
        COMMENT '현재 Python worker 처리 시도 식별자'
        AFTER summary_status,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0
        COMMENT 'AI 요약 처리 재시도 횟수'
        AFTER processing_attempt_id,
    ADD COLUMN processing_started_at DATETIME NULL
        COMMENT '현재 worker가 처리를 시작한 시각'
        AFTER retry_count,
    ADD COLUMN completed_at DATETIME NULL
        COMMENT '요약 완료 또는 최종 실패 시각'
        AFTER error_message,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0
        COMMENT '동시 수정 방지용 낙관적 잠금 버전'
        AFTER updated_at;

-- 개인 초안 정책을 적용할 수 있도록 과거 데이터의 요청자를 우선 복원한다.
UPDATE bid_notice_summary summary_table
SET summary_table.requested_by = COALESCE(
        summary_table.requested_by,
        summary_table.confirmed_by,
        (SELECT MIN(employee.user_id) FROM employee)
    )
WHERE summary_table.requested_by IS NULL;

UPDATE bid_notice_summary summary_table
    LEFT JOIN employee requester
        ON requester.user_id = summary_table.requested_by
SET summary_table.company_id = COALESCE(requester.company_id, 1),
    summary_table.prompt = COALESCE(
        summary_table.prompt,
        '[이관 데이터: 요청 프롬프트 없음]'
    ),
    summary_table.notice_snapshot = JSON_OBJECT(
        'noticeId', summary_table.bid_notice_id,
        'migratedLegacySummary', TRUE
    ),
    summary_table.processing_attempt_id = COALESCE(
        summary_table.processing_attempt_id,
        UUID()
    )
WHERE summary_table.company_id IS NULL
   OR summary_table.prompt IS NULL
   OR summary_table.notice_snapshot IS NULL
   OR summary_table.processing_attempt_id IS NULL;

-- 과거 데이터가 완료 상태가 아니거나 확정 필드가 불완전하면 미확정 상태로 정규화한다.
UPDATE bid_notice_summary
SET confirmed = FALSE,
    confirmed_by = NULL,
    confirmed_at = NULL
WHERE confirmed = FALSE
   OR summary_status <> 'COMPLETED'
   OR confirmed_by IS NULL
   OR confirmed_at IS NULL;

UPDATE bid_notice_summary
SET completed_at = COALESCE(completed_at, updated_at, created_at)
WHERE summary_status IN ('COMPLETED', 'FAILED')
  AND completed_at IS NULL;

-- requested_by의 NULL 허용 여부를 바꾸기 위해 기존 FK를 먼저 분리한다.
ALTER TABLE bid_notice_summary
    DROP FOREIGN KEY fk_bid_notice_summary_requester;

ALTER TABLE bid_notice_summary
    MODIFY COLUMN company_id BIGINT NOT NULL
        COMMENT '요약을 소유한 회사(테넌트)',
    MODIFY COLUMN requested_by VARCHAR(20) NOT NULL
        COMMENT 'AI 요약을 요청하고 미확정 초안을 소유한 사용자',
    MODIFY COLUMN prompt TEXT NOT NULL
        COMMENT '사용자가 입력한 요약 프롬프트 원문',
    MODIFY COLUMN notice_snapshot JSON NOT NULL
        COMMENT '요약 요청 시점의 입찰 공고 및 첨부 메타데이터 스냅샷',
    MODIFY COLUMN processing_attempt_id CHAR(36) NOT NULL
        COMMENT '현재 Python worker 처리 시도 식별자',
    ADD COLUMN active_processing_marker TINYINT
        GENERATED ALWAYS AS (
            CASE
                WHEN summary_status IN ('PENDING', 'PROCESSING')
                     AND deleted_at IS NULL
                    THEN 1
                ELSE NULL
            END
        ) STORED
        COMMENT '회사·공고·요청자별 진행 중 요청 중복 방지 표식'
        AFTER version,
    ADD CONSTRAINT fk_bid_notice_summary_company
        FOREIGN KEY (company_id)
        REFERENCES company (company_id),
    ADD CONSTRAINT fk_bid_notice_summary_requester
        FOREIGN KEY (requested_by)
        REFERENCES employee (user_id),
    ADD CONSTRAINT fk_bid_notice_summary_project
        FOREIGN KEY (project_id)
        REFERENCES project (project_id),
    ADD CONSTRAINT chk_bid_notice_summary_status
        CHECK (summary_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    ADD CONSTRAINT chk_bid_notice_summary_retry_count
        CHECK (retry_count >= 0 AND retry_count <= 3),
    ADD CONSTRAINT chk_bid_notice_summary_prompt_length
        CHECK (CHAR_LENGTH(prompt) BETWEEN 1 AND 3000),
    ADD CONSTRAINT chk_bid_notice_summary_confirmation
        CHECK (
            (confirmed = FALSE AND confirmed_by IS NULL AND confirmed_at IS NULL)
            OR
            (
                confirmed = TRUE
                AND summary_status = 'COMPLETED'
                AND confirmed_by IS NOT NULL
                AND confirmed_at IS NOT NULL
            )
        ),
    ADD CONSTRAINT chk_bid_notice_summary_project_link
        CHECK (
            project_id IS NULL
            OR (summary_status = 'COMPLETED' AND confirmed = TRUE)
        ),
    ADD CONSTRAINT uk_bid_notice_summary_active_processing
        UNIQUE (
            company_id,
            bid_notice_id,
            requested_by,
            active_processing_marker
        ),
    ADD CONSTRAINT uk_bid_notice_summary_project
        UNIQUE (project_id),
    ADD KEY idx_bid_notice_summary_company_history (
        company_id,
        bid_notice_id,
        confirmed,
        confirmed_at,
        created_at
    ),
    ADD KEY idx_bid_notice_summary_requester_history (
        company_id,
        requested_by,
        bid_notice_id,
        created_at
    ),
    ADD KEY idx_bid_notice_summary_worker_attempt (
        summary_status,
        processing_attempt_id
    );

-- 요약 요청 저장과 같은 트랜잭션에서 생성하고, Dispatcher가 Redis Stream으로 발행한다.
CREATE TABLE bid_notice_summary_outbox (
    bid_notice_summary_outbox_id BIGINT NOT NULL AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL
        COMMENT 'Outbox 이벤트 멱등 식별자',
    bid_notice_summary_id BIGINT NOT NULL,
    attempt_id CHAR(36) NOT NULL
        COMMENT '발행 대상 AI 요약 처리 시도 식별자',
    event_type VARCHAR(50) NOT NULL,
    payload JSON NOT NULL
        COMMENT 'summaryId, companyId, attemptId, retryCount만 포함한 Redis 메시지',
    publish_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    publish_attempt_count INT NOT NULL DEFAULT 0,
    available_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_owner VARCHAR(100) NULL,
    lock_expires_at DATETIME NULL,
    published_at DATETIME NULL,
    last_error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (bid_notice_summary_outbox_id),
    CONSTRAINT uk_bid_notice_summary_outbox_event
        UNIQUE (event_id),
    CONSTRAINT uk_bid_notice_summary_outbox_attempt
        UNIQUE (bid_notice_summary_id, attempt_id),
    CONSTRAINT fk_bid_notice_summary_outbox_summary
        FOREIGN KEY (bid_notice_summary_id)
        REFERENCES bid_notice_summary (bid_notice_summary_id)
        ON DELETE RESTRICT,
    CONSTRAINT chk_bid_notice_summary_outbox_status
        CHECK (publish_status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT chk_bid_notice_summary_outbox_attempt_count
        CHECK (
            publish_attempt_count >= 0
            AND publish_attempt_count <= 5
        ),
    KEY idx_bid_notice_summary_outbox_claim (
        publish_status,
        available_at,
        lock_expires_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='입찰 AI 요약 Redis 발행 Outbox';
-- 기존 상태 이력은 작업자 소속 회사로 가능한 범위에서 보정합니다.
ALTER TABLE bid_notice_status_history
    ADD COLUMN company_id BIGINT NULL
        COMMENT '상태 변경 대상 회사'
        AFTER bid_notice_status_history_id;

UPDATE bid_notice_status_history history
    JOIN employee employee_table
ON employee_table.user_id = history.changed_by
    SET history.company_id = employee_table.company_id
WHERE history.company_id IS NULL;

ALTER TABLE bid_notice_status_history
    ADD KEY idx_bid_notice_status_history_company_notice
    (company_id, bid_notice_id, created_at),
    ADD CONSTRAINT fk_bid_notice_status_history_company
        FOREIGN KEY (company_id)
        REFERENCES company (company_id);
