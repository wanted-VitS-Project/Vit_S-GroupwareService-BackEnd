-- VitamateFileIndexRetryScheduler가 5분마다 index_status=PENDING AND updated_at < :before로
-- 정체된 파일 인덱싱을 재시도 대상으로 훑는데, file_index에 PK 외 인덱스가 전혀 없어
-- 파일이 늘어날수록 매 주기마다 전체 스캔이 된다.
ALTER TABLE file_index
    ADD INDEX idx_file_index_pending_scan (index_status, updated_at);
