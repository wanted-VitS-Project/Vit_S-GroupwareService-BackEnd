ALTER TABLE document_chunk
    ADD COLUMN excerpt VARCHAR(1000) NULL COMMENT 'AI 분석 입력 및 근거 표시용 청크 미리보기'
    AFTER chroma_id;