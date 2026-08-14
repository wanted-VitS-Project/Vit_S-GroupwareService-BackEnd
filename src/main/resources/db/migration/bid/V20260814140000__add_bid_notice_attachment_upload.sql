-- 직접 등록 공고 첨부를 "공개 링크"뿐 아니라 실제 파일 업로드로도 받을 수 있게 확장한다.
-- 링크형은 source_url만, 업로드형은 storage_key만 채운다 - 정확히 하나만 채워야 한다.
ALTER TABLE bid_notice_attachment
    MODIFY COLUMN source_url VARCHAR(1000) NULL,
    ADD COLUMN storage_key VARCHAR(1000) NULL AFTER source_url,
    ADD COLUMN upload_status VARCHAR(20) NOT NULL DEFAULT 'READY' AFTER storage_key,
    ADD COLUMN size_bytes BIGINT NULL AFTER upload_status,
    ADD COLUMN mime_type VARCHAR(100) NULL AFTER size_bytes;

ALTER TABLE bid_notice_attachment
    ADD CONSTRAINT chk_bid_notice_attachment_source
        CHECK (
            (source_url IS NOT NULL AND storage_key IS NULL)
            OR (source_url IS NULL AND storage_key IS NOT NULL)
        ),
    ADD CONSTRAINT chk_bid_notice_attachment_upload_status
        CHECK (upload_status IN ('UPLOADING', 'READY', 'FAILED'));
