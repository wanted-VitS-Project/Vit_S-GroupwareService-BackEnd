-- =====================================================================
-- 멀티테넌트 Phase 1 · Activity Log 직접 격리
-- =====================================================================
-- 활동 로그는 회사별 감사 타임라인이므로 project 하위 조회 여부와 별개로 행 자체에
-- company_id를 저장한다. DEFAULT 1은 기존 단일 회사 로그의 백필과 배포 전환을 위한
-- 임시값이며, 모든 기록 경로의 명시적 스탬핑을 운영에서 확인한 뒤 별도 마이그레이션으로 제거한다.

ALTER TABLE activity_log
    ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1
        COMMENT '회사(테넌트) · 기록 경로 검증 후 DEFAULT 제거' AFTER activity_log_id,
    ADD KEY idx_activity_log_company_block_cursor (company_id, block_id, activity_log_id),
    ADD CONSTRAINT fk_activity_log_company
        FOREIGN KEY (company_id) REFERENCES company (company_id);
