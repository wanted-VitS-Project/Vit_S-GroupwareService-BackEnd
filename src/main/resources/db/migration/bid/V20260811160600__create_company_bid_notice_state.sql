CREATE TABLE company_bid_notice_state (
    company_bid_notice_state_id BIGINT NOT NULL AUTO_INCREMENT,
    company_id BIGINT NOT NULL,
    bid_notice_id BIGINT NOT NULL,
    notice_status VARCHAR(20) NOT NULL DEFAULT 'COLLECTED',
    dismiss_reason VARCHAR(500) NULL,
    first_seen_run_id BIGINT NULL,
    last_seen_run_id BIGINT NULL,
    first_seen_at DATETIME NOT NULL,
    last_seen_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    PRIMARY KEY (company_bid_notice_state_id),
    UNIQUE KEY uk_company_bid_notice_state (company_id, bid_notice_id),
    KEY idx_company_bid_notice_state_list (company_id, notice_status, bid_notice_id),
    KEY idx_company_bid_notice_state_first_run (first_seen_run_id),
    CONSTRAINT fk_company_bid_notice_state_company
        FOREIGN KEY (company_id) REFERENCES company (company_id),
    CONSTRAINT fk_company_bid_notice_state_notice
        FOREIGN KEY (bid_notice_id) REFERENCES bid_notice (bid_notice_id),
    CONSTRAINT fk_company_bid_notice_state_first_run
        FOREIGN KEY (first_seen_run_id) REFERENCES crawl_run (crawl_run_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_company_bid_notice_state_last_run
        FOREIGN KEY (last_seen_run_id) REFERENCES crawl_run (crawl_run_id)
        ON DELETE SET NULL
);

-- 기존 수집 이력에서 회사별 최초/최종 발견 상태를 복원합니다.
INSERT INTO company_bid_notice_state (
    company_id,
    bid_notice_id,
    notice_status,
    first_seen_run_id,
    last_seen_run_id,
    first_seen_at,
    last_seen_at,
    created_at,
    updated_at
)
SELECT
    condition_table.company_id,
    raw.bid_notice_id,
    'COLLECTED',
    MIN(raw.crawl_run_id),
    MAX(raw.crawl_run_id),
    MIN(raw.created_at),
    MAX(raw.created_at),
    MIN(raw.created_at),
    MAX(raw.created_at)
FROM bid_notice_raw raw
JOIN crawl_run run_table
  ON run_table.crawl_run_id = raw.crawl_run_id
JOIN crawl_condition condition_table
  ON condition_table.crawl_condition_id = run_table.crawl_condition_id
WHERE raw.deleted_at IS NULL
  AND raw.crawl_run_id IS NOT NULL
GROUP BY condition_table.company_id, raw.bid_notice_id;
