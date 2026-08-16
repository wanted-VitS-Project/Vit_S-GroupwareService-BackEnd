-- VitamateFileIndexRetryScheduler가 updated_at 경과 시간만으로 "유실"을 판단해, 아직 살아있는
-- worker(Gemini 429 백오프로 처리가 오래 걸리는 경우 등)의 시도를 빼앗아 attemptId를 재발급하는
-- 레이스가 있었다. crawl_run_task와 동일하게 진짜 lease로 교체한다 — dispatch·PROCESSING 확인
-- 시점에 명시적으로 만료 시각을 박아두고, 그 시각이 실제로 지나야만 재claim을 허용한다.
ALTER TABLE file_index
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '자동 재시도 횟수' AFTER index_error_message,
    ADD COLUMN processing_started_at DATETIME NULL COMMENT '현재 시도가 시작된 시각' AFTER retry_count,
    ADD COLUMN lease_expires_at DATETIME NULL COMMENT '현재 시도의 점유 만료 시각 — 이 시각이 지나야 재claim 가능' AFTER processing_started_at;

ALTER TABLE file_index
    ADD CONSTRAINT chk_file_index_retry_count CHECK (retry_count >= 0 AND retry_count <= 2);

ALTER TABLE file_index
    ADD INDEX idx_file_index_worker_claim (index_status, lease_expires_at);
