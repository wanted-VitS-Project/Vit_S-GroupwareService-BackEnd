-- Outbox 발행이 영구 실패한 작업을 종료 상태로 관리한다.
ALTER TABLE crawl_run_outbox
DROP CHECK chk_crawl_run_outbox_status,
    ADD CONSTRAINT chk_crawl_run_outbox_status
        CHECK (publish_status IN ('PENDING', 'PUBLISHED', 'FAILED'));

-- Dispatcher의 무한 발행 재시도를 방지한다.
ALTER TABLE crawl_run_outbox
DROP CHECK chk_crawl_run_outbox_attempt_count,
    ADD CONSTRAINT chk_crawl_run_outbox_attempt_count
        CHECK (
            publish_attempt_count >= 0
            AND publish_attempt_count <= 5
        );

-- 수집 실행 물리 삭제로 Outbox 장애 이력이 함께 삭제되지 않게 보호한다.
ALTER TABLE crawl_run_outbox
DROP FOREIGN KEY fk_crawl_run_outbox_run,
    ADD CONSTRAINT fk_crawl_run_outbox_run_restrict
        FOREIGN KEY (crawl_run_id)
        REFERENCES crawl_run (crawl_run_id)
        ON DELETE RESTRICT;

-- 복합 UNIQUE 인덱스의 좌측 접두사와 중복되는 단일 인덱스를 제거한다.
ALTER TABLE crawl_run_outbox
DROP INDEX idx_crawl_run_outbox_run;
