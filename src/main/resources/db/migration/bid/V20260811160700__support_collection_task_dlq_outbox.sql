-- Task 영구 실패와 DLQ 발행 대기를 같은 Outbox에서 관리합니다.
ALTER TABLE crawl_run_outbox
    ADD COLUMN crawl_run_task_id BIGINT NULL AFTER crawl_run_id,
    ADD COLUMN deduplication_key VARCHAR(150)
        GENERATED ALWAYS AS (
            CASE
                WHEN crawl_run_task_id IS NULL
                    THEN CONCAT('RUN:', crawl_run_id, ':', attempt_id)
                ELSE CONCAT('TASK:', crawl_run_task_id, ':', attempt_id)
            END
        ) STORED AFTER attempt_id,
    ADD CONSTRAINT fk_crawl_run_outbox_task
        FOREIGN KEY (crawl_run_task_id)
        REFERENCES crawl_run_task (crawl_run_task_id)
        ON DELETE RESTRICT;

-- 기존 Run Job과 Task DLQ는 서로 다른 멱등성 경계를 사용합니다.
ALTER TABLE crawl_run_outbox
    ADD INDEX idx_crawl_run_outbox_run (crawl_run_id),
    DROP INDEX uk_crawl_run_outbox_attempt,
    ADD CONSTRAINT uk_crawl_run_outbox_delivery
        UNIQUE (event_type, deduplication_key);
