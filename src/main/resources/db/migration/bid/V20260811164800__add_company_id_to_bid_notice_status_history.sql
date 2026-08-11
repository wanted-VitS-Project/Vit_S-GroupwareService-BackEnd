-- 회사별 입찰 공고 제외·복구 이력을 격리할 수 있도록 테넌트 식별자를 추가합니다.
-- 기존 이력은 소유 회사를 확정할 수 없으므로 company_id의 NULL을 허용합니다.
ALTER TABLE bid_notice_status_history
    ADD COLUMN company_id BIGINT NULL AFTER bid_notice_id,
    ADD KEY idx_bid_notice_status_history_company_notice_created (
        company_id,
        bid_notice_id,
        created_at
    ),
    ADD CONSTRAINT fk_bid_notice_status_history_company
        FOREIGN KEY (company_id)
        REFERENCES company (company_id);
