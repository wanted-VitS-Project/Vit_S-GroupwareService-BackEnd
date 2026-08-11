-- 실행 접수 이후 수집 조건이 수정되어도 당시 조건으로 처리할 수 있도록 스냅샷을 저장한다.
-- Worker의 중복 처리와 오래된 처리 결과 반영을 막기 위한 시도 식별자를 관리한다.
ALTER TABLE crawl_run
    ADD COLUMN condition_snapshot JSON NULL
        COMMENT '실행 요청 당시 수집처와 검색 조건 스냅샷'
        AFTER crawl_condition_id,
    ADD COLUMN processing_attempt_id CHAR(36) NULL
        COMMENT '현재 Worker 처리 시도 식별자'
        AFTER run_status,
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0
        COMMENT '외부 수집 처리 재시도 횟수'
        AFTER processing_attempt_id,
    ADD COLUMN processing_started_at DATETIME NULL
        COMMENT '현재 Worker가 처리를 시작한 시각'
        AFTER retry_count,
    ADD COLUMN lease_expires_at DATETIME NULL
        COMMENT 'Worker 점유 만료 시각'
        AFTER processing_started_at,
    ADD COLUMN error_code VARCHAR(100) NULL
        COMMENT '마지막 수집 오류 코드'
        AFTER skipped_count,
    ADD COLUMN updated_at DATETIME NOT NULL
        DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
    COMMENT '수집 실행 수정 시각'
                                                      AFTER created_at;

-- 기존 실행 데이터에는 현재 연결된 수집 조건을 기준으로 스냅샷을 채운다.
UPDATE crawl_run cr
    JOIN crawl_condition cc
ON cc.crawl_condition_id = cr.crawl_condition_id
    JOIN crawl_source cs
    ON cs.crawl_source_id = cc.crawl_source_id
    SET cr.condition_snapshot = JSON_OBJECT(
        'sourceCode', cs.source_code,
        'conditionName', cc.condition_name,
        'noticeTypes', COALESCE(JSON_EXTRACT(cc.params, '$.noticeTypes'), JSON_ARRAY()),
        'filters', COALESCE(JSON_EXTRACT(cc.params, '$.filters'), JSON_OBJECT())
        )
WHERE cr.condition_snapshot IS NULL;

ALTER TABLE crawl_run
    MODIFY COLUMN condition_snapshot JSON NOT NULL
    COMMENT '실행 요청 당시 수집처와 검색 조건 스냅샷',
    ADD CONSTRAINT chk_crawl_run_retry_count
    CHECK (retry_count >= 0),
    ADD KEY idx_crawl_run_worker_claim (
    run_status,
    lease_expires_at
    );

-- crawl_run 저장과 같은 트랜잭션에서 생성되는 Redis 발행 이벤트다.
CREATE TABLE crawl_run_outbox (
                                  crawl_run_outbox_id BIGINT NOT NULL AUTO_INCREMENT,
                                  event_id CHAR(36) NOT NULL
                                      COMMENT 'Outbox 이벤트의 멱등 식별자',
                                  crawl_run_id BIGINT NOT NULL,
                                  attempt_id CHAR(36) NOT NULL
                                      COMMENT 'Worker 처리 시도 식별자',
                                  event_type VARCHAR(50) NOT NULL,
                                  payload JSON NOT NULL
                                      COMMENT 'Redis Stream에 전달할 최소 작업 정보',
                                  publish_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                                  publish_attempt_count INT NOT NULL DEFAULT 0,
                                  available_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  lock_owner VARCHAR(100) NULL,
                                  lock_expires_at DATETIME NULL,
                                  published_at DATETIME NULL,
                                  last_error_message VARCHAR(500) NULL,
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME NOT NULL
                                                                      DEFAULT CURRENT_TIMESTAMP
                                      ON UPDATE CURRENT_TIMESTAMP,

                                  PRIMARY KEY (crawl_run_outbox_id),

                                  CONSTRAINT uk_crawl_run_outbox_event
                                      UNIQUE (event_id),

                                  CONSTRAINT uk_crawl_run_outbox_attempt
                                      UNIQUE (crawl_run_id, attempt_id),

                                  CONSTRAINT fk_crawl_run_outbox_run
                                      FOREIGN KEY (crawl_run_id)
                                          REFERENCES crawl_run (crawl_run_id)
                                          ON DELETE CASCADE,

                                  CONSTRAINT chk_crawl_run_outbox_status
                                      CHECK (publish_status IN ('PENDING', 'PUBLISHED')),

                                  CONSTRAINT chk_crawl_run_outbox_attempt_count
                                      CHECK (publish_attempt_count >= 0),

                                  KEY idx_crawl_run_outbox_run (crawl_run_id),

                                  KEY idx_crawl_run_outbox_claim (
        publish_status,
        available_at,
        lock_expires_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COMMENT='입찰 공고 수집 작업 Redis 발행 Outbox';
