-- 입찰 AI 요약의 개선 이력을 부모 요약과 개정 번호로 연결한다.
-- 기존 요약은 모두 독립적인 1차 요약으로 유지한다.
ALTER TABLE bid_notice_summary
    ADD COLUMN parent_summary_id BIGINT NULL
        COMMENT '개선 요청의 기준이 된 이전 AI 요약'
        AFTER bid_notice_id,
    ADD COLUMN revision_no INT NOT NULL DEFAULT 1
        COMMENT '요약 개선 계보 안의 개정 번호(1~20)'
        AFTER parent_summary_id,
    ADD CONSTRAINT fk_bid_notice_summary_parent
        FOREIGN KEY (parent_summary_id)
        REFERENCES bid_notice_summary (bid_notice_summary_id)
        ON DELETE RESTRICT,
    ADD CONSTRAINT chk_bid_notice_summary_revision_no
        CHECK (revision_no BETWEEN 1 AND 20),
    ADD KEY idx_bid_notice_summary_parent (parent_summary_id),
    ADD KEY idx_bid_notice_summary_history_page (
        company_id,
        bid_notice_id,
        created_at,
        bid_notice_summary_id
    );
