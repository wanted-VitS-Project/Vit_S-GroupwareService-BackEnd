-- VitamateFileIndexRetryScheduler가 updated_at 경과 시간만으로 "유실"을 판단해, 아직 살아있는
-- worker(Gemini 429 백오프로 처리가 오래 걸리는 경우 등)의 시도를 빼앗아 attemptId를 재발급하는
-- 레이스가 있었다. crawl_run_task와 동일하게 진짜 lease로 교체한다 — dispatch·PROCESSING 확인
-- 시점에 명시적으로 만료 시각을 박아두고, 그 시각이 실제로 지나야만 재claim을 허용한다.
ALTER TABLE file_index
    ADD COLUMN retry_count INT NOT NULL DEFAULT 0 COMMENT '자동 재시도 횟수' AFTER index_error_message,
    ADD COLUMN processing_started_at DATETIME NULL COMMENT '현재 시도가 시작된 시각' AFTER retry_count,
    ADD COLUMN lease_expires_at DATETIME NULL COMMENT '현재 시도의 점유 만료 시각 — 이 시각이 지나야 재claim 가능' AFTER processing_started_at;

-- ⚠️ 배포 시점에 이미 PENDING/PROCESSING인 행은 lease_expires_at이 NULL로 시작되는데,
-- 재시도 스케줄러의 후보 조회는 lease_expires_at IS NOT NULL을 요구한다. 백필하지 않으면
-- 이 행들은 워커가 죽어도 영원히 재claim도 최종 실패 처리도 안 되고 방치된다 — updated_at
-- 기반 옛 판정 로직이 이 마이그레이션에서 완전히 대체되므로 다른 안전망이 없다.
UPDATE file_index
   SET processing_started_at = updated_at,
       lease_expires_at = updated_at
 WHERE index_status IN ('PENDING', 'PROCESSING')
   AND lease_expires_at IS NULL;

-- ⚠️ 상한을 DB CHECK로 못박으면 정책값(FileIndexLeasePolicy.MAX_RETRY_COUNT)을 바꿀 때마다
-- 반드시 새 마이그레이션이 따라와야 한다. 실제 상한 판정은 애플리케이션 쿼리 조건
-- (retry_count < :maxRetryCount)이 담당하므로, DB 제약은 음수 방지 정도만 남긴다.
ALTER TABLE file_index
    ADD CONSTRAINT chk_file_index_retry_count CHECK (retry_count >= 0);

ALTER TABLE file_index
    ADD INDEX idx_file_index_worker_claim (index_status, lease_expires_at);
