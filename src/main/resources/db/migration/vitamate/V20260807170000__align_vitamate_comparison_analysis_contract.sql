ALTER TABLE vitamate_analysis_document
    ADD COLUMN document_role VARCHAR(20) NOT NULL DEFAULT 'TARGET' AFTER file_version_id;

ALTER TABLE vitamate_analysis_document
    ADD CONSTRAINT chk_vitamate_analysis_document_role
        CHECK (document_role IN ('REFERENCE', 'TARGET'));

-- 최종 프롬프트와 분석별 템플릿 스냅샷으로 대체된 중복 컬럼을 제거한다.
ALTER TABLE vitamate_analysis
    DROP COLUMN additional_instruction,
    DROP COLUMN prompt_template_version;
