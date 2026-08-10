ALTER TABLE crawl_condition
    ADD COLUMN company_id BIGINT NOT NULL DEFAULT 1
    COMMENT '수집 조건을 소유한 회사'
        AFTER crawl_condition_id,
    ADD KEY idx_crawl_condition_company_deleted (
        company_id,
        deleted_at
    ),
    ADD CONSTRAINT fk_crawl_condition_company
        FOREIGN KEY (company_id)
        REFERENCES company (company_id);