-- 나라장터 공사·용역 공고의 정규화 필드와 반복 첨부파일을 저장한다.
-- 기존 공고는 분류 정보를 복원할 수 없으므로 신규 컬럼의 NULL을 허용한다.

ALTER TABLE bid_notice
    MODIFY COLUMN notice_name VARCHAR(1000) NOT NULL,
    MODIFY COLUMN notice_agency VARCHAR(400) NULL,
    MODIFY COLUMN demand_agency VARCHAR(400) NULL,
    ADD COLUMN notice_type VARCHAR(20) NULL
        COMMENT '공고 유형: CONSTRUCTION, SERVICE'
        AFTER notice_ord,
    ADD COLUMN external_notice_status VARCHAR(30) NULL
        COMMENT '나라장터 공고 상태명: 등록공고, 변경공고, 취소공고, 재공고'
        AFTER notice_name,
    ADD COLUMN international_bid_type VARCHAR(20) NULL
        COMMENT '국내·국제 입찰 구분: DOMESTIC, INTERNATIONAL'
        AFTER external_notice_status,
    ADD COLUMN bid_method VARCHAR(100) NULL
        COMMENT '입찰 방식: 전자입찰, 직찰 등'
        AFTER joint_contract_text,
    ADD KEY idx_bid_notice_type_status (notice_type, external_notice_status),
    ADD KEY idx_bid_notice_deadline (bid_deadline_at, deleted_at);

CREATE TABLE bid_notice_attachment (
    bid_notice_attachment_id BIGINT NOT NULL AUTO_INCREMENT,
    bid_notice_id BIGINT NOT NULL,
    attachment_kind VARCHAR(30) NOT NULL DEFAULT 'NOTICE_SPEC'
        COMMENT '첨부 유형: NOTICE_SPEC 등',
    attachment_order SMALLINT UNSIGNED NOT NULL
        COMMENT '외부 응답의 첨부 순서',
    file_name VARCHAR(500) NOT NULL
        COMMENT '나라장터 제공 파일명',
    source_url VARCHAR(1000) NOT NULL
        COMMENT '나라장터 원문 다운로드 링크',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NULL,
    deleted_at DATETIME NULL,
    PRIMARY KEY (bid_notice_attachment_id),
    CONSTRAINT uk_bid_notice_attachment_order
        UNIQUE (bid_notice_id, attachment_kind, attachment_order),
    KEY idx_bid_notice_attachment_active (bid_notice_id, deleted_at),
    CONSTRAINT fk_bid_notice_attachment_notice
        FOREIGN KEY (bid_notice_id)
        REFERENCES bid_notice (bid_notice_id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  COMMENT='입찰 공고 외부 첨부파일';
