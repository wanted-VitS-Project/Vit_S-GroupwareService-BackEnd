-- 직접 등록 공고의 회사 소유권과 회사별 중복 방지 키를 추가한다.
-- 외부 수집 공고는 공용 데이터이므로 두 컬럼을 NULL로 유지한다.
ALTER TABLE bid_notice
    ADD COLUMN owner_company_id BIGINT NULL
        COMMENT '직접 등록 공고 소유 회사. 외부 수집 공고는 NULL'
        AFTER crawl_source_id,
    ADD COLUMN manual_dedup_key CHAR(64) NULL
        COMMENT '직접 등록 공고의 회사별 정규화 중복 키 SHA-256'
        AFTER notice_ord,
    ADD KEY idx_bid_notice_owner (owner_company_id, deleted_at),
    ADD CONSTRAINT uk_bid_notice_manual_dedup
        UNIQUE (owner_company_id, manual_dedup_key),
    ADD CONSTRAINT fk_bid_notice_owner_company
        FOREIGN KEY (owner_company_id)
        REFERENCES company (company_id);

-- MANUAL은 수집 조건에서 사용하지 않고 직접 등록 공고의 출처 식별에만 사용한다.
INSERT INTO crawl_source (
    source_code,
    source_name,
    source_type,
    enabled,
    created_at
)
VALUES (
    'MANUAL',
    '직접 등록',
    'MANUAL',
    TRUE,
    CURRENT_TIMESTAMP
)
ON DUPLICATE KEY UPDATE
    source_name = VALUES(source_name),
    source_type = VALUES(source_type),
    enabled = VALUES(enabled),
    deleted_at = NULL,
    updated_at = CURRENT_TIMESTAMP;
