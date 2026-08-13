-- 입찰 검토 기준자료를 사내 문서함(company_document_version)에서도 선택할 수 있도록 확장.
-- bid_reference_file_id는 이미 fk_bid_review_document_reference로 잠겨 있어 재사용 불가 -> 새 컬럼.
ALTER TABLE bid_review_document
    ADD COLUMN company_document_version_id BIGINT NULL
        COMMENT '사내 문서함 참조 버전 ID(company_document_version) - 참조 선택 새 경로'
        AFTER bid_reference_file_id;

ALTER TABLE bid_review_document
    DROP CHECK chk_bid_review_document_role;

ALTER TABLE bid_review_document
    ADD CONSTRAINT chk_bid_review_document_role
        CHECK (document_role IN (
            'BID_ATTACHMENT',
            'INTERNAL_REFERENCE',
            'COMPANY_DOCUMENT_REFERENCE'
        ));

ALTER TABLE bid_review_document
    DROP CHECK chk_bid_review_document_source;

ALTER TABLE bid_review_document
    ADD CONSTRAINT chk_bid_review_document_source
        CHECK (
            (document_role = 'BID_ATTACHMENT'
                AND bid_notice_attachment_id IS NOT NULL
                AND bid_reference_file_id IS NULL
                AND company_document_version_id IS NULL)
            OR (document_role = 'INTERNAL_REFERENCE'
                AND bid_notice_attachment_id IS NULL
                AND bid_reference_file_id IS NOT NULL
                AND company_document_version_id IS NULL)
            OR (document_role = 'COMPANY_DOCUMENT_REFERENCE'
                AND bid_notice_attachment_id IS NULL
                AND bid_reference_file_id IS NULL
                AND company_document_version_id IS NOT NULL)
        );

ALTER TABLE bid_review_document
    ADD CONSTRAINT fk_bid_review_document_company_doc_version
        FOREIGN KEY (company_document_version_id)
            REFERENCES company_document_version (company_document_version_id)
            ON DELETE RESTRICT;

ALTER TABLE bid_review_document
    ADD CONSTRAINT uk_bid_review_document_company_doc_version
        UNIQUE (bid_review_id, company_document_version_id);
